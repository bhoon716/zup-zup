#!/usr/bin/env python3
"""Offline standard-library tests. No Docker, network, or real host capacity reads."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("storage_health", Path(__file__).with_name("check-storage-health.py"))
PROBE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROBE)
PRIVATE_PATH = "/private/sensitive-path"


class StorageHealthTest(unittest.TestCase):
    def setUp(self):
        self.options = PROBE.parser().parse_args([])
        self.stats = SimpleNamespace(f_frsize=PROBE.GIB, f_blocks=100, f_bavail=20,
                                     f_files=1000, f_favail=200)

    def check(self):
        with patch.object(PROBE.os, "statvfs", return_value=self.stats):
            return PROBE.check("release", PRIVATE_PATH, self.options)

    def test_available_not_root_reserved_bytes_and_inodes_are_reported(self):
        event = self.check()
        self.assertEqual(event["status"], "ok")
        self.assertEqual(event["freeBytes"], 20 * PROBE.GIB)
        self.assertEqual(event["freePercent"], 20)
        self.assertEqual(event["freeInodes"], 200)
        self.assertEqual(event["freeInodePercent"], 20)
        self.assertEqual(event["schemaVersion"], 1)
        self.assertNotIn(PRIVATE_PATH, json.dumps(event))

    def test_each_threshold_is_independently_enforced(self):
        for field, value, violation in (
            ("min_free_bytes", 21 * PROBE.GIB, "free_bytes_low"),
            ("min_free_percent", 21, "free_percent_low"),
            ("min_free_inode_percent", 21, "free_inodes_low"),
        ):
            with self.subTest(field=field):
                self.options = PROBE.parser().parse_args([])
                setattr(self.options, field, value)
                self.assertEqual(self.check()["violations"], [violation])
                self.assertEqual(self.check()["status"], "low")

    def test_exact_threshold_is_healthy_and_exhaustion_is_not(self):
        self.options.min_free_bytes = 20 * PROBE.GIB
        self.options.min_free_percent = 20
        self.options.min_free_inode_percent = 20
        self.assertEqual(self.check()["status"], "ok")
        self.stats.f_bavail = 0
        self.stats.f_favail = 0
        self.assertEqual(len(self.check()["violations"]), 3)

    def test_missing_permission_and_other_os_errors_never_leak_paths(self):
        for error, reason in ((FileNotFoundError(PRIVATE_PATH), "path_missing"),
                              (PermissionError(PRIVATE_PATH), "path_inaccessible"),
                              (OSError(PRIVATE_PATH), "stat_failed")):
            with self.subTest(reason=reason), patch.object(PROBE.os, "statvfs", side_effect=error):
                event = PROBE.check("docker_data", PRIVATE_PATH, self.options)
                self.assertEqual(event["status"], "error")
                self.assertEqual(event["reason"], reason)
                self.assertNotIn(PRIVATE_PATH, json.dumps(event))

    def test_unknown_or_invalid_counters_fail_closed(self):
        for field, value in (("f_files", 0), ("f_blocks", 0), ("f_frsize", 0),
                             ("f_favail", -1), ("f_bavail", -1),
                             ("f_favail", 1001), ("f_bavail", 101)):
            with self.subTest(field=field):
                self.setUp()
                setattr(self.stats, field, value)
                self.assertEqual(self.check()["reason"], "counters_unavailable")

    def test_invalid_thresholds_are_rejected_without_echoing_input(self):
        for flag, value in (("--min-free-bytes", "0"), ("--min-free-bytes", "-1"),
                            ("--min-free-bytes", "1.5"), ("--min-free-percent", "nan"),
                            ("--min-free-percent", "inf"), ("--min-free-percent", "101"),
                            ("--min-free-inode-percent", "0"),
                            ("--min-free-percent", PRIVATE_PATH)):
            output = io.StringIO()
            with self.subTest(flag=flag, value=value), contextlib.redirect_stderr(output):
                with self.assertRaises(SystemExit) as caught:
                    PROBE.main([flag, value])
                self.assertEqual(caught.exception.code, 2)
                self.assertNotIn(PRIVATE_PATH, output.getvalue())

    def test_both_roles_report_even_when_first_fails_and_exit_is_nonzero(self):
        output = io.StringIO()
        with patch.object(PROBE.os, "statvfs", side_effect=[PermissionError(), self.stats]), contextlib.redirect_stdout(output):
            result = PROBE.main([])
        events = [json.loads(line) for line in output.getvalue().splitlines()]
        self.assertEqual(result, 1)
        self.assertEqual([event["role"] for event in events], list(PROBE.ROLES))
        self.assertEqual([event["status"] for event in events], ["error", "ok"])

    def test_success_exit_and_explicit_paths(self):
        output = io.StringIO()
        with patch.object(PROBE.os, "statvfs", return_value=self.stats) as statvfs, contextlib.redirect_stdout(output):
            result = PROBE.main(["--release-path", PRIVATE_PATH, "--docker-data-path", PRIVATE_PATH])
        self.assertEqual(result, 0)
        self.assertEqual(statvfs.call_count, 2)
        statvfs.assert_called_with(PRIVATE_PATH)
        self.assertNotIn(PRIVATE_PATH, output.getvalue())

    def test_role_labels_are_bounded(self):
        with self.assertRaises(ValueError):
            PROBE.check(PRIVATE_PATH, PRIVATE_PATH, self.options)


if __name__ == "__main__":
    unittest.main()
