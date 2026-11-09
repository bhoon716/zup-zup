#!/usr/bin/env python3
"""Exercise static preflight with temporary non-credential files only."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("alerting_settings", Path(__file__).with_name("validate-alerting-settings.py"))
SETTINGS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SETTINGS)


class AlertingSettingsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        self.release = root / "release"
        self.release.mkdir()
        self.secret = root / "operator-secret-fixture"
        self.secret.touch()
        self.targets = root / "targets.json"
        self.targets.write_text('[{"targets":["alertmanager:9093"]}]')
        self.settings = {"ALERTMANAGER_IMAGE": "prom/alertmanager@sha256:" + "a" * 64,
                         "ALERTMANAGER_SLACK_WEBHOOK_FILE": str(self.secret),
                         "PROMETHEUS_ALERTMANAGER_TARGETS_FILE": str(self.targets),
                         "ALERTING_RELEASE_ROOT": str(self.release)}

    def reason(self):
        return SETTINGS.validate_settings(self.settings)

    def test_valid_settings_do_not_open_secret_contents(self):
        original_read = Path.read_text

        def tracked_read(path, *args, **kwargs):
            self.assertNotEqual(path, self.secret)
            return original_read(path, *args, **kwargs)

        with patch.object(Path, "read_text", tracked_read):
            self.assertEqual(self.reason(), "validated")

    def test_image_requires_official_repository_and_full_lowercase_digest(self):
        for value in ("prom/alertmanager:latest", "prom/alertmanager:v0.28.1", "example.test/evil@sha256:" + "a" * 64,
                      "prom/alertmanager@sha256:short", "prom/alertmanager@sha256:" + "A" * 64):
            self.settings["ALERTMANAGER_IMAGE"] = value
            self.assertEqual(self.reason(), "image_digest_invalid")

    def test_missing_release_root_or_relative_root_fails_closed(self):
        for value in ("", "release"):
            self.settings["ALERTING_RELEASE_ROOT"] = value
            self.assertEqual(self.reason(), "release_root_invalid")

    def test_secret_and_targets_must_exist_as_regular_files_outside_release(self):
        internal = self.release / "fixture"
        internal.touch()
        for key, prefix in (("ALERTMANAGER_SLACK_WEBHOOK_FILE", "secret"),
                            ("PROMETHEUS_ALERTMANAGER_TARGETS_FILE", "targets")):
            original = self.settings[key]
            for path, reason in ((str(internal), prefix + "_within_release"),
                                 ("relative", prefix + "_path_invalid"),
                                 (str(self.release.parent / "absent"), prefix + "_path_invalid"),
                                 (str(self.release.parent), prefix + "_path_invalid")):
                with self.subTest(key=key, path=path):
                    self.settings[key] = path
                    self.assertEqual(self.reason(), reason)
            self.settings[key] = original

    def test_symlink_into_release_is_rejected(self):
        internal = self.release / "fixture"
        internal.touch()
        self.secret.unlink()
        self.secret.symlink_to(internal)
        self.assertEqual(self.reason(), "secret_within_release")

    def test_discovery_accepts_only_the_single_canonical_internal_target(self):
        for value in ([], [{"targets": ["example.test:9093"]}], [{"targets": ["alertmanager:9093"], "labels": {"secret": "x"}}],
                      [{"targets": ["alertmanager:9093", "other:9093"]}], {}, "not-json"):
            with self.subTest(value=value):
                self.targets.write_text(json.dumps(value))
                self.assertEqual(self.reason(), "targets_content_invalid")
        self.targets.write_text("{" + " " * 4096)
        self.assertEqual(self.reason(), "targets_content_invalid")

    def test_malformed_json_and_unreadable_discovery_are_redacted(self):
        self.targets.write_text("private-fixture-content")
        self.assertEqual(self.reason(), "targets_content_invalid")
        with patch.object(Path, "read_text", side_effect=PermissionError("private-value")):
            self.assertEqual(self.reason(), "targets_content_invalid")

    def test_invalid_ports_fail_closed(self):
        for value in ("0", "65536", "bad", "9093:9093", "0.0.0.0:9093"):
            self.settings["ALERTMANAGER_PORT_HOST"] = value
            self.assertEqual(self.reason(), "port_invalid")

    def test_empty_port_uses_the_same_default_as_compose(self):
        self.settings["ALERTMANAGER_PORT_HOST"] = ""
        self.assertEqual(self.reason(), "validated")

    def test_discovery_cannot_point_to_secret_or_its_symlink(self):
        self.targets.unlink()
        self.targets.symlink_to(self.secret)
        with patch.object(Path, "read_text") as read:
            self.assertEqual(self.reason(), "targets_secret_overlap")
        read.assert_not_called()

    def test_cli_outputs_status_only_without_paths_or_values(self):
        output = io.StringIO()
        self.settings["ALERTMANAGER_IMAGE"] = "private-fixture-value"
        with patch.dict(os.environ, self.settings, clear=True), contextlib.redirect_stdout(output):
            code = SETTINGS.main()
        self.assertEqual(code, 1)
        event = json.loads(output.getvalue())
        self.assertEqual(event["status"], "invalid")
        self.assertEqual(event["reason"], "image_digest_invalid")
        self.assertNotIn("private-fixture-value", output.getvalue())
        self.assertNotIn(str(self.secret), output.getvalue())


if __name__ == "__main__":
    unittest.main()
