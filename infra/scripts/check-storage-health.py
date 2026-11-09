#!/usr/bin/env python3
"""Read-only host filesystem capacity probe; emits one JSON event per fixed role."""
import argparse
from datetime import datetime, timezone
import json
import math
import os

GIB = 1024 ** 3
ROLES = ("release", "docker_data")


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        # argparse 오류에 경로나 사용자 입력을 포함하지 않는다.
        self.exit(2, "저장 공간 점검 인자가 올바르지 않습니다. --help를 확인하세요.\n")


def positive_bytes(value):
    number = int(value)
    if number <= 0:
        raise ValueError("positive bytes required")
    return number


def percent(value):
    number = float(value)
    if not math.isfinite(number) or not 0 < number <= 100:
        raise ValueError("percent must be in (0, 100]")
    return number


def parser():
    result = SafeParser(description="호스트 저장 공간과 inode 여유 점검 (읽기 전용)")
    result.add_argument("--release-path", default="/home/ubuntu/jbnu-sugang-helper")
    result.add_argument("--docker-data-path", default="/var/lib/docker")
    result.add_argument("--min-free-bytes", type=positive_bytes, default=5 * GIB)
    result.add_argument("--min-free-percent", type=percent, default=15.0)
    result.add_argument("--min-free-inode-percent", type=percent, default=10.0)
    return result


def measurements(stats):
    if (stats.f_frsize <= 0 or stats.f_blocks <= 0 or stats.f_files <= 0
            or not 0 <= stats.f_bavail <= stats.f_blocks
            or not 0 <= stats.f_favail <= stats.f_files):
        raise ValueError("unavailable filesystem counters")
    return {
        "totalBytes": stats.f_blocks * stats.f_frsize,
        "freeBytes": stats.f_bavail * stats.f_frsize,
        "freePercent": stats.f_bavail * 100.0 / stats.f_blocks,
        "totalInodes": stats.f_files,
        "freeInodes": stats.f_favail,
        "freeInodePercent": stats.f_favail * 100.0 / stats.f_files,
    }


def check(role, path, options):
    if role not in ROLES:
        raise ValueError("unsupported role")
    event = {
        "event": "host_storage_health", "schemaVersion": 1,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "role": role, "status": "error", "reason": "stat_failed",
        "minFreeBytes": options.min_free_bytes,
        "minFreePercent": options.min_free_percent,
        "minFreeInodePercent": options.min_free_inode_percent,
    }
    try:
        event.update(measurements(os.statvfs(path)))
    except FileNotFoundError:
        event["reason"] = "path_missing"
        return event
    except PermissionError:
        event["reason"] = "path_inaccessible"
        return event
    except OSError:
        return event
    except (ValueError, OverflowError):
        event["reason"] = "counters_unavailable"
        return event
    failures = []
    if event["freeBytes"] < options.min_free_bytes:
        failures.append("free_bytes_low")
    if event["freePercent"] < options.min_free_percent:
        failures.append("free_percent_low")
    if event["freeInodePercent"] < options.min_free_inode_percent:
        failures.append("free_inodes_low")
    event["status"] = "ok"
    event["reason"] = "within_thresholds"
    event["violations"] = failures
    if failures:
        event["status"] = "low"
        event["reason"] = "threshold_breached"
    return event


def emit_event(event):
    print(json.dumps(event, ensure_ascii=False, allow_nan=False, sort_keys=True))


def main(argv=None):
    options = parser().parse_args(argv)
    targets = (("release", options.release_path), ("docker_data", options.docker_data_path))
    exit_code = 0
    for role, path in targets:
        event = check(role, path, options)
        emit_event(event)
        if event["status"] != "ok":
            exit_code = 1
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
