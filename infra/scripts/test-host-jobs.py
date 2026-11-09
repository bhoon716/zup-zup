#!/usr/bin/env python3
"""Offline scheduled-job tests; never access Docker, production, or real disk stats."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import pwd
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

SPEC = importlib.util.spec_from_file_location("host_jobs", Path(__file__).with_name("run-host-job.py"))
JOBS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(JOBS)
PRIVATE = "/private/do-not-log"
SECRET = "fixture-secret-do-not-log"


class HostJobsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.state = Path(self.temporary.name)
        self.stats = SimpleNamespace(f_frsize=1024 ** 3, f_blocks=100, f_bavail=20,
                                     f_files=1000, f_favail=200)
        self.output = io.StringIO()

    def run_job(self, task="storage", **extra):
        with patch.object(JOBS.PROBE.os, "statvfs", return_value=self.stats), \
                contextlib.redirect_stdout(self.output):
            return JOBS.main([task, "--state-directory", str(self.state),
                              "--release-path", PRIVATE, "--docker-data-path", PRIVATE,
                              *extra.get("args", [])])

    def metrics(self, task="storage"):
        return (self.state / (task + ".prom")).read_text()

    def test_storage_publishes_atomic_metrics_and_redacted_independent_fields(self):
        # given / when
        self.assertEqual(self.run_job(), 0)
        events = [json.loads(line) for line in self.output.getvalue().splitlines()]
        # then
        self.assertEqual(events[-1]["event"], "host_job_completed")
        self.assertEqual(events[-1]["task"], "storage")
        self.assertEqual(events[-1]["status"], "ok")
        self.assertNotIn(PRIVATE, self.output.getvalue() + self.metrics())
        self.assertIn('sugang_host_job_success{task="storage"} 1', self.metrics())
        self.assertIn('sugang_host_storage_free_bytes{role="release"} 21474836480', self.metrics())
        self.assertEqual(list(self.state.glob("*.tmp")), [])

    def test_failure_preserves_last_success_and_drops_unavailable_measurements(self):
        # given
        self.assertEqual(self.run_job(), 0)
        previous = JOBS._read_last_success(self.state / "storage.prom", "storage")
        # when
        with patch.object(JOBS.PROBE.os, "statvfs", side_effect=PermissionError(PRIVATE)), \
                contextlib.redirect_stdout(self.output):
            result = JOBS.main(["storage", "--state-directory", str(self.state)])
        # then
        self.assertEqual(result, 1)
        self.assertEqual(JOBS._read_last_success(self.state / "storage.prom", "storage"), previous)
        self.assertIn('sugang_host_job_success{task="storage"} 0', self.metrics())
        self.assertNotIn("sugang_host_storage_free_bytes", self.metrics())
        self.assertIn('sugang_host_storage_measurement_success{role="release"} 0', self.metrics())
        self.assertNotIn(PRIVATE, self.output.getvalue())

    def test_backup_requires_secret_before_launch_and_never_logs_it(self):
        with patch.dict(JOBS.os.environ, {}, clear=True), patch.object(JOBS, "_run_process") as run:
            self.assertEqual(self.run_job("backup"), 1)
        run.assert_not_called()
        self.assertIn('"reason": "credential_missing"', self.output.getvalue())

    def test_backup_publishes_in_progress_then_success_without_child_output(self):
        def process(command, timeout, environment):
            self.assertIn('sugang_host_job_success{task="backup"} 0', self.metrics("backup"))
            self.assertIn('sugang_host_job_running{task="backup"} 1', self.metrics("backup"))
            self.assertEqual(environment["DB_ROOT_PASSWORD"], SECRET)
            self.assertNotIn("UNRELATED_SECRET", environment)
            self.assertEqual(command[-1], str(Path(JOBS.__file__).with_name("backup-server-export.sh")))
            return 0
        with patch.dict(JOBS.os.environ, {"DB_ROOT_PASSWORD": SECRET, "UNRELATED_SECRET": SECRET}), \
                patch.object(JOBS, "_run_process", side_effect=process):
            self.assertEqual(self.run_job("backup"), 0)
        self.assertNotIn(SECRET, self.output.getvalue() + self.metrics("backup"))
        self.assertIn('sugang_host_job_running{task="backup"} 0', self.metrics("backup"))

    def test_backup_blocked_on_low_storage_without_launching_child(self):
        self.stats.f_bavail = 1
        with patch.dict(JOBS.os.environ, {"DB_ROOT_PASSWORD": SECRET}), patch.object(JOBS, "_run_process") as run:
            self.assertEqual(self.run_job("backup"), 1)
        run.assert_not_called()
        self.assertIn('"reason": "storage_unhealthy"', self.output.getvalue())

    def test_subprocess_failure_and_timeout_do_not_refresh_success(self):
        for failure in (1, subprocess.TimeoutExpired([SECRET], 10), OSError(SECRET)):
            with self.subTest(failure=type(failure).__name__):
                mock_options = {"return_value": failure}
                if isinstance(failure, Exception):
                    mock_options = {"side_effect": failure}
                with patch.object(JOBS, "_run_process", **mock_options):
                    self.assertEqual(self.run_job("event_backup"), 1)
                self.assertEqual(JOBS._read_last_success(self.state / "event_backup.prom", "event_backup"), 0)
        self.assertNotIn(SECRET, self.output.getvalue())

    def test_event_exporter_does_not_receive_database_credentials(self):
        with patch.dict(JOBS.os.environ, {"DB_ROOT_PASSWORD": SECRET}), \
                patch.object(JOBS, "_run_process", return_value=0) as run:
            self.assertEqual(self.run_job("event_backup"), 0)
        command, timeout, environment = run.call_args.args
        self.assertEqual(Path(command[1]).name, "backup-important-events.py")
        self.assertEqual(timeout, 240)
        self.assertNotIn("DB_ROOT_PASSWORD", environment)

    def test_already_running_does_not_overwrite_active_metrics(self):
        # given
        self.assertEqual(self.run_job(), 0)
        before = self.metrics()
        with (self.state / "storage.lock").open("a") as lock:
            JOBS.fcntl.flock(lock.fileno(), JOBS.fcntl.LOCK_EX | JOBS.fcntl.LOCK_NB)
            # when / then
            self.assertEqual(self.run_job(), 1)
        self.assertEqual(self.metrics(), before)
        self.assertIn('"reason": "already_running"', self.output.getvalue())

    def test_missing_state_directory_fails_without_creating_or_leaking_path(self):
        with contextlib.redirect_stdout(self.output):
            result = JOBS.main(["storage", "--state-directory", str(self.state / "absent")])
        self.assertEqual(result, 1)
        self.assertFalse((self.state / "absent").exists())
        self.assertNotIn(str(self.state), self.output.getvalue())

    def test_invalid_or_future_previous_success_is_not_trusted(self):
        target = self.state / "storage.prom"
        for value in ("nan", "inf", "-1", "9999999999999999", "sensitive"):
            target.write_text('sugang_host_job_last_success_timestamp_seconds{task="storage"} ' + value + '\n')
            self.assertEqual(JOBS._read_last_success(target, "storage"), 0)

    def test_metrics_write_failure_returns_failure_without_exposing_exception(self):
        with patch.object(JOBS, "_publish_metrics", side_effect=OSError(SECRET)):
            self.assertEqual(self.run_job(), 1)
        self.assertNotIn(SECRET, self.output.getvalue())
        self.assertIn('"reason": "state_unavailable"', self.output.getvalue())

    def test_atomic_publish_keeps_previous_file_if_replace_fails(self):
        # given
        self.assertEqual(self.run_job(), 0)
        before = self.metrics()
        # when
        with patch.object(JOBS.os, "replace", side_effect=OSError(SECRET)):
            self.assertEqual(self.run_job(), 1)
        # then
        self.assertEqual(self.metrics(), before)
        self.assertEqual(list(self.state.glob("*.tmp")), [])

    def test_backup_forwards_only_explicit_export_scope_and_upload_path(self):
        with patch.dict(JOBS.os.environ, {"DB_ROOT_PASSWORD": SECRET,
                                          "BACKUP_INCLUDE_UPLOADS": "0", "UPLOADS_VOL_PATH": PRIVATE}), \
                patch.object(JOBS, "_run_process", return_value=0) as run:
            self.assertEqual(self.run_job("backup"), 0)
        environment = run.call_args.args[2]
        self.assertEqual(environment["BACKUP_INCLUDE_UPLOADS"], "0")
        self.assertEqual(environment["UPLOADS_VOL_PATH"], PRIVATE)
        self.assertNotIn(PRIVATE, self.output.getvalue())

    def test_templates_have_explicit_bounds_and_do_not_install_themselves(self):
        root = Path(__file__).resolve().parents[2]
        config = root / "infra/systemd"
        for name, task, timeout in (("storage", "storage", "60"),
                                    ("backup", "backup", "16min"),
                                    ("event-backup", "event_backup", "270")):
            service = (config / ("sugang-helper-" + name + ".service")).read_text()
            timer = (config / ("sugang-helper-" + name + ".timer")).read_text()
            self.assertIn("run-host-job.py " + task, service)
            self.assertIn("TimeoutStartSec=" + timeout, service)
            self.assertIn("KillMode=control-group", service)
            self.assertIn("ProtectSystem=strict", service)
            self.assertIn("PrivateNetwork=true", service)
            self.assertIn("StateDirectory=sugang-helper-host-jobs", service)
            self.assertIn("StateDirectoryMode=0750", service)
            self.assertIn("User=root", service)
            self.assertIn("Group=root", service)
            self.assertIn("Persistent=true", timer)
            self.assertIn("Unit=sugang-helper-" + name + ".service", timer)
            self.assertNotIn("ExecStartPre", service)
            self.assertNotIn("Restart=", service)
            if name in ("backup", "event-backup"):
                self.assertIn("Environment=HOST_EXPORT_USER=ubuntu", service)
            if name != "backup":
                self.assertNotIn("/jbnu-sugang-helper/.env", service)
        self.assertIn("19:00:00 UTC", (config / "sugang-helper-backup.timer").read_text())
        self.assertIn("*:0/5:00 UTC", (config / "sugang-helper-storage.timer").read_text())
        self.assertIn("*:0/5:00 UTC", (config / "sugang-helper-event-backup.timer").read_text())

    def test_root_exporter_uses_existing_offhost_readers_identity_and_groups(self):
        account = SimpleNamespace(pw_name="ubuntu", pw_uid=1000, pw_gid=1000)
        process = MagicMock()
        process.wait.return_value = 0
        with patch.object(JOBS.os, "geteuid", return_value=0), \
                patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                patch.object(pwd, "getpwnam", return_value=account) as lookup, \
                patch.object(JOBS.os, "getgrouplist", return_value=[1000, 999, 4]) as groups, \
                patch.object(JOBS.subprocess, "Popen", return_value=process) as start:
            self.assertEqual(JOBS._run_process(["fixture"], 1, {}), 0)
        lookup.assert_called_once_with("ubuntu")
        groups.assert_called_once_with("ubuntu", 1000)
        self.assertEqual(start.call_args.kwargs["user"], 1000)
        self.assertEqual(start.call_args.kwargs["group"], 1000)
        self.assertEqual(start.call_args.kwargs["extra_groups"], [4, 999, 1000])
        self.assertNotIn("preexec_fn", start.call_args.kwargs)

    def test_non_root_direct_execution_preserves_current_identity(self):
        process = MagicMock()
        process.wait.return_value = 0
        with patch.object(JOBS.os, "geteuid", return_value=501), \
                patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                patch.object(pwd, "getpwnam") as lookup, \
                patch.object(JOBS.subprocess, "Popen", return_value=process) as start:
            self.assertEqual(JOBS._run_process(["fixture"], 1, {}), 0)
        lookup.assert_not_called()
        self.assertTrue({"user", "group", "extra_groups", "preexec_fn"}.isdisjoint(start.call_args.kwargs))

    def test_root_requires_explicit_fixed_non_root_export_owner(self):
        for user in ("", "root", "other-account"):
            with self.subTest(user=user), patch.object(JOBS.os, "geteuid", return_value=0), \
                    patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": user}), \
                    patch.object(JOBS.subprocess, "Popen") as start:
                self.assertEqual(self.run_job("event_backup"), 1)
            start.assert_not_called()
        self.assertIn('"reason": "export_identity_unavailable"', self.output.getvalue())
        self.assertEqual(JOBS._read_last_success(self.state / "event_backup.prom", "event_backup"), 0)

    def test_missing_export_account_never_falls_back_to_root(self):
        with patch.object(JOBS.os, "geteuid", return_value=0), \
                patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                patch.object(pwd, "getpwnam", side_effect=KeyError(SECRET)), \
                patch.object(JOBS.subprocess, "Popen") as start:
            self.assertEqual(self.run_job("event_backup"), 1)
        start.assert_not_called()
        self.assertNotIn(SECRET, self.output.getvalue())
        self.assertIn('"reason": "export_identity_unavailable"', self.output.getvalue())

    def test_account_resolving_to_root_is_rejected(self):
        for uid, gid in ((0, 1000), (1000, 0)):
            account = SimpleNamespace(pw_name="ubuntu", pw_uid=uid, pw_gid=gid)
            with self.subTest(uid=uid, gid=gid), patch.object(JOBS.os, "geteuid", return_value=0), \
                    patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                    patch.object(pwd, "getpwnam", return_value=account), \
                    patch.object(JOBS.subprocess, "Popen") as start:
                self.assertEqual(self.run_job("event_backup"), 1)
            start.assert_not_called()

    def test_group_lookup_failure_is_redacted_and_never_uses_root_groups(self):
        account = SimpleNamespace(pw_name="ubuntu", pw_uid=1000, pw_gid=1000)
        with patch.object(JOBS.os, "geteuid", return_value=0), \
                patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                patch.object(pwd, "getpwnam", return_value=account), \
                patch.object(JOBS.os, "getgrouplist", side_effect=OSError(SECRET)), \
                patch.object(JOBS.subprocess, "Popen") as start:
            self.assertEqual(self.run_job("event_backup"), 1)
        start.assert_not_called()
        self.assertNotIn(SECRET, self.output.getvalue())
        self.assertIn('"reason": "export_identity_unavailable"', self.output.getvalue())

    def test_root_supplementary_group_or_empty_identity_groups_are_rejected(self):
        account = SimpleNamespace(pw_name="ubuntu", pw_uid=1000, pw_gid=1000)
        for groups in ([1000, 0], []):
            with self.subTest(groups=groups), patch.object(JOBS.os, "geteuid", return_value=0), \
                    patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                    patch.object(pwd, "getpwnam", return_value=account), \
                    patch.object(JOBS.os, "getgrouplist", return_value=groups), \
                    patch.object(JOBS.subprocess, "Popen") as start:
                self.assertEqual(self.run_job("event_backup"), 1)
            start.assert_not_called()
        self.assertIn('"reason": "export_identity_unavailable"', self.output.getvalue())

    def test_privilege_drop_permission_failure_does_not_retry_as_root(self):
        account = SimpleNamespace(pw_name="ubuntu", pw_uid=1000, pw_gid=1000)
        with patch.object(JOBS.os, "geteuid", return_value=0), \
                patch.dict(JOBS.os.environ, {"HOST_EXPORT_USER": "ubuntu"}), \
                patch.object(pwd, "getpwnam", return_value=account), \
                patch.object(JOBS.os, "getgrouplist", return_value=[1000, 999]), \
                patch.object(JOBS.subprocess, "Popen", side_effect=PermissionError(SECRET)) as start:
            self.assertEqual(self.run_job("event_backup"), 1)
        self.assertEqual(start.call_count, 1)
        self.assertEqual(start.call_args.kwargs["user"], 1000)
        self.assertIn('"reason": "export_unavailable"', self.output.getvalue())
        self.assertNotIn(SECRET, self.output.getvalue())
        self.assertEqual(JOBS._read_last_success(self.state / "event_backup.prom", "event_backup"), 0)

    def test_timeout_kills_process_group_and_reaps_child(self):
        process = MagicMock()
        process.pid = 12345
        process.wait.side_effect = [subprocess.TimeoutExpired([SECRET], 1), 0]
        with patch.object(JOBS.os, "geteuid", return_value=501), \
                patch.object(JOBS.subprocess, "Popen", return_value=process) as start, \
                patch.object(JOBS.os, "killpg") as kill:
            with self.assertRaises(subprocess.TimeoutExpired):
                JOBS._run_process(["fixture"], 1, {})
        self.assertTrue(start.call_args.kwargs["start_new_session"])
        self.assertIs(start.call_args.kwargs["stdout"], subprocess.DEVNULL)
        self.assertIs(start.call_args.kwargs["stderr"], subprocess.DEVNULL)
        kill.assert_called_once_with(12345, JOBS.signal.SIGKILL)
        self.assertEqual(process.wait.call_count, 2)


if __name__ == "__main__":
    unittest.main()
