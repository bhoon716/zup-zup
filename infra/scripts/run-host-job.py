#!/usr/bin/env python3
"""Run fixed host maintenance jobs and atomically expose numeric status to Alloy."""
from datetime import datetime, timezone
import fcntl
import importlib.util
import json
import math
import os
from pathlib import Path
import pwd
import signal
import subprocess
import sys
import tempfile
import time

SPEC = importlib.util.spec_from_file_location("storage_health", Path(__file__).with_name("check-storage-health.py"))
PROBE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROBE)
TASKS = ("storage", "backup", "event_backup")


def _parser():
    result = PROBE.parser()
    result.description = "호스트 주기 작업과 제한된 상태 지표 게시"
    result.add_argument("task", choices=TASKS)
    result.add_argument("--state-directory", default="/var/lib/sugang-helper-host-jobs")
    result.set_defaults(release_path=os.environ.get("RELEASE_ROOT", "/home/ubuntu/jbnu-sugang-helper"),
                        docker_data_path=os.environ.get("DOCKER_DATA_PATH", "/var/lib/docker"))
    return result


def _log_job_result(task, status, reason, duration):
    print(json.dumps({"event": "host_job_completed", "schemaVersion": 1,
                      "timestamp": datetime.now(timezone.utc).isoformat(),
                      "task": task, "status": status, "reason": reason,
                      "durationSeconds": round(duration, 3),
                      "message": "호스트 주기 작업 결과"}, ensure_ascii=False, sort_keys=True), flush=True)


def _read_last_success(target, task):
    try:
        if target.is_symlink():
            return 0
        with target.open() as handle:
            data = handle.read(65537)
        if len(data) > 65536:
            return 0
        prefix = 'sugang_host_job_last_success_timestamp_seconds{task="' + task + '"} '
        values = [line[len(prefix):] for line in data.splitlines() if line.startswith(prefix)]
        if len(values) != 1:
            return 0
        value = float(values[0])
        if math.isfinite(value) and 0 <= value <= time.time():
            return value
    except (OSError, ValueError):
        return 0
    return 0


def _storage_metrics(events):
    lines = []
    for event in events:
        role = event["role"]
        suffix = '{role="' + role + '"}'
        measured = int(event["status"] != "error")
        lines.append("sugang_host_storage_measurement_success" + suffix + " " + str(measured))
        lines.append("sugang_host_storage_healthy" + suffix + " " + str(int(event["status"] == "ok")))
        if not measured:
            continue
        for key, metric in (("freeBytes", "free_bytes"), ("freePercent", "free_percent"),
                            ("freeInodePercent", "free_inode_percent")):
            lines.append("sugang_host_storage_" + metric + suffix + " " + str(event[key]))
    return lines


def _publish_metrics(target, task, success, running, started, completed, last_success, duration, events):
    suffix = '{task="' + task + '"}'
    metrics = (("success", success), ("running", running),
               ("last_started_timestamp_seconds", started),
               ("last_completed_timestamp_seconds", completed),
               ("last_success_timestamp_seconds", last_success), ("duration_seconds", duration))
    lines = ["sugang_host_job_" + name + suffix + " " + str(value) for name, value in metrics]
    if task == "storage":
        lines.extend(_storage_metrics(events))
    descriptor, temporary = tempfile.mkstemp(prefix=task + ".", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(descriptor, "w") as handle:
            handle.write("\n".join(lines) + "\n")
            handle.flush()
            os.fsync(handle.fileno())
            # Numeric, fixed-cardinality telemetry only; never credentials or paths.
            os.fchmod(handle.fileno(), 0o644)
        os.replace(temporary, target)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def _check_storage(options):
    events = []
    for role, path in (("release", options.release_path), ("docker_data", options.docker_data_path)):
        event = PROBE.check(role, path, options)
        PROBE.emit_event(event)
        events.append(event)
    return events


def _child_environment(options):
    # Do not forward unrelated runtime secrets or Docker remote-host overrides.
    return {"PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "LANG": "C.UTF-8", "RELEASE_ROOT": options.release_path}


class _ExportIdentityUnavailable(Exception):
    """The existing off-host backup owner cannot safely run the export."""


def _export_identity():
    if os.geteuid() != 0:
        return {}
    # The production SSH/deploy contract uses the existing ubuntu account.
    # Never silently create root-private archives that its rsync cannot read.
    name = os.environ.get("HOST_EXPORT_USER")
    if name != "ubuntu":
        raise _ExportIdentityUnavailable()
    try:
        account = pwd.getpwnam(name)
        if account.pw_uid <= 0 or account.pw_gid <= 0:
            raise _ExportIdentityUnavailable()
        groups = sorted(set(os.getgrouplist(name, account.pw_gid)))
        if not groups or any(group <= 0 for group in groups):
            raise _ExportIdentityUnavailable()
    except (KeyError, OSError, ValueError):
        raise _ExportIdentityUnavailable() from None
    return {"user": account.pw_uid, "group": account.pw_gid, "extra_groups": groups}


def _run_process(command, timeout, environment):
    process = subprocess.Popen(command, env=environment, stdin=subprocess.DEVNULL,
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                               start_new_session=True, **_export_identity())
    try:
        return process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait()
        raise


def _execute_job(options, events):
    if any(event["status"] != "ok" for event in events):
        return "storage_unhealthy"
    if options.task == "storage":
        return "completed"
    environment = _child_environment(options)
    command = [sys.executable, str(Path(__file__).with_name("backup-important-events.py")),
               "--release-root", options.release_path, "--timeout-seconds", "210"]
    timeout = 240
    if options.task == "backup":
        password = os.environ.get("DB_ROOT_PASSWORD")
        if not password:
            return "credential_missing"
        environment["DB_ROOT_PASSWORD"] = password
        # Exporter owns validation of this explicit no-uploads option.
        if "BACKUP_INCLUDE_UPLOADS" in os.environ:
            environment["BACKUP_INCLUDE_UPLOADS"] = os.environ["BACKUP_INCLUDE_UPLOADS"]
        if "UPLOADS_VOL_PATH" in os.environ:
            environment["UPLOADS_VOL_PATH"] = os.environ["UPLOADS_VOL_PATH"]
        command = ["/bin/bash", str(Path(__file__).with_name("backup-server-export.sh"))]
        timeout = 900
    try:
        if _run_process(command, timeout, environment) != 0:
            return "export_failed"
    except _ExportIdentityUnavailable:
        return "export_identity_unavailable"
    except subprocess.TimeoutExpired:
        return "export_timed_out"
    except OSError:
        return "export_unavailable"
    return "completed"


def _run_locked(options):
    started = time.time()
    monotonic_start = time.monotonic()
    target = Path(options.state_directory) / (options.task + ".prom")
    previous_success = _read_last_success(target, options.task)
    _publish_metrics(target, options.task, 0, 1, started, 0, previous_success, 0, [])
    events = _check_storage(options)
    reason = _execute_job(options, events)
    completed = time.time()
    duration = max(0, time.monotonic() - monotonic_start)
    success = int(reason == "completed")
    if success:
        previous_success = completed
    _publish_metrics(target, options.task, success, 0, started, completed, previous_success, duration, events)
    status = "error"
    if success:
        status = "ok"
    _log_job_result(options.task, status, reason, duration)
    return 1 - success


def main(argv=None):
    options = _parser().parse_args(argv)
    try:
        lock_path = Path(options.state_directory) / (options.task + ".lock")
        with os.fdopen(os.open(lock_path, os.O_CREAT | os.O_WRONLY | os.O_NOFOLLOW, 0o600), "w") as lock:
            try:
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                _log_job_result(options.task, "error", "already_running", 0)
                return 1
            return _run_locked(options)
    except OSError:
        _log_job_result(options.task, "error", "state_unavailable", 0)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
