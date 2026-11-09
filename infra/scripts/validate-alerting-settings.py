#!/usr/bin/env python3
"""Static opt-in preflight. Never reads the webhook file or contacts a service."""
import json
import os
from pathlib import Path
import re

CANONICAL_TARGETS = [{"targets": ["alertmanager:9093"]}]


def resolve_regular_file(value):
    path = Path(value)
    if not value or not path.is_absolute():
        return None
    try:
        path = path.resolve(strict=True)
        if path.is_file():
            return path
    except (OSError, RuntimeError):
        return None
    return None


def validate_operator_file(settings, key, role, release):
    path = resolve_regular_file(settings.get(key, ""))
    if path is None:
        return role + "_path_invalid"
    if path.is_relative_to(release):
        return role + "_within_release"
    return "validated"


def validate_targets(value):
    try:
        path = Path(value)
        if path.stat().st_size > 4096:
            return "targets_content_invalid"
        if json.loads(path.read_text(encoding="utf-8")) != CANONICAL_TARGETS:
            return "targets_content_invalid"
    except (OSError, ValueError, UnicodeError):
        return "targets_content_invalid"
    return "validated"


def validate_settings(settings):
    if not re.fullmatch(r"prom/alertmanager@sha256:[a-f0-9]{64}", settings.get("ALERTMANAGER_IMAGE", "")):
        return "image_digest_invalid"
    root_value = settings.get("ALERTING_RELEASE_ROOT", "")
    if not root_value or not Path(root_value).is_absolute():
        return "release_root_invalid"
    try:
        release = Path(root_value).resolve(strict=True)
        if not release.is_dir():
            return "release_root_invalid"
    except (OSError, RuntimeError):
        return "release_root_invalid"
    for key, role in (("ALERTMANAGER_SLACK_WEBHOOK_FILE", "secret"),
                      ("PROMETHEUS_ALERTMANAGER_TARGETS_FILE", "targets")):
        reason = validate_operator_file(settings, key, role, release)
        if reason != "validated":
            return reason
    if Path(settings["ALERTMANAGER_SLACK_WEBHOOK_FILE"]).resolve() == Path(settings["PROMETHEUS_ALERTMANAGER_TARGETS_FILE"]).resolve():
        return "targets_secret_overlap"
    port = settings.get("ALERTMANAGER_PORT_HOST") or "9093"
    if not re.fullmatch(r"[0-9]{1,5}", port) or not 1 <= int(port) <= 65535:
        return "port_invalid"
    return validate_targets(settings["PROMETHEUS_ALERTMANAGER_TARGETS_FILE"])


def _log_validation_result(status, reason):
    print(json.dumps({"event": "alerting_configuration", "schemaVersion": 1,
                      "status": status, "reason": reason}, sort_keys=True))


def main():
    reason = validate_settings(os.environ)
    status = "valid"
    exit_code = 0
    if reason != "validated":
        status = "invalid"
        exit_code = 1
    _log_validation_result(status, reason)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
