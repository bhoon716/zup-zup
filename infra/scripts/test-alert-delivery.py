#!/usr/bin/env python3
"""Offline tests: no Docker, credentials, real receiver or socket access."""
import contextlib
from datetime import datetime, timezone
import importlib.util
import io
import json
from pathlib import Path
import unittest
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("alert_delivery", Path(__file__).with_name("test-alert-receiver.py"))
DELIVERY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(DELIVERY)
TEST_ID = "fixture-20261108"
PRIVATE_VALUE = "private-response-never-log"
NOW = datetime(2026, 11, 8, 12, 0, tzinfo=timezone.utc)


class AlertDeliveryTest(unittest.TestCase):
    def invoke(self, *args):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = DELIVERY.main(["--test-id", TEST_ID, *args])
        return code, json.loads(output.getvalue())

    def test_default_is_preview_and_does_not_open_receiver(self):
        with patch.object(DELIVERY, "send_alert") as send:
            code, event = self.invoke()
        send.assert_not_called()
        self.assertEqual(code, 0)
        self.assertEqual(event["status"], "preview")
        self.assertEqual(event["phase"], "firing")
        self.assertFalse(event["receiptVerified"])

    def test_sending_requires_both_explicit_switches(self):
        for arguments in (["--send"], ["--confirm-send"]):
            with self.subTest(arguments=arguments), patch.object(DELIVERY, "send_alert") as send:
                code, event = self.invoke(*arguments)
            self.assertEqual(code, 2)
            self.assertEqual(event["reason"], "send_confirmation_required")
            send.assert_not_called()

    def test_payload_has_fixed_labels_and_bounded_timestamps(self):
        firing = DELIVERY.build_alert(TEST_ID, "firing", NOW)
        resolved = DELIVERY.build_alert(TEST_ID, "resolved", NOW)
        self.assertEqual(firing["labels"], resolved["labels"])
        self.assertEqual(firing["labels"], {"alertname": "AlertDeliverySmokeTest", "severity": "info",
                                         "service": "sugang-helper", "test_id": TEST_ID})
        self.assertEqual(firing["endsAt"], "2026-11-08T12:10:00Z")
        self.assertEqual(resolved["endsAt"], "2026-11-08T12:00:00Z")
        self.assertEqual(firing["annotations"]["summary"], "TEST: 합성 알림 수신 경로 점검")
        self.assertEqual(resolved["annotations"]["summary"], "RESOLVED: 합성 알림 수신 경로 점검 종료")

    def test_post_is_json_and_uses_no_proxy_or_redirects(self):
        response = MagicMock()
        response.status = 200
        response.__enter__.return_value = response
        opener = MagicMock()
        opener.open.return_value = response
        payload = DELIVERY.build_alert(TEST_ID, "firing", NOW)
        with patch.object(DELIVERY, "build_opener", return_value=opener) as build:
            status = DELIVERY.send_alert("http://127.0.0.1:9093", payload)
        self.assertEqual(status, 200)
        self.assertEqual(build.call_args.args[0].proxies, {})
        self.assertIsInstance(build.call_args.args[1], DELIVERY.NoRedirect)
        request = opener.open.call_args.args[0]
        self.assertEqual(request.full_url, "http://127.0.0.1:9093/api/v2/alerts")
        self.assertEqual(request.method, "POST")
        self.assertEqual(json.loads(request.data), [payload])
        self.assertEqual(opener.open.call_args.kwargs["timeout"], 10)
        response.read.assert_not_called()

    def test_redirect_is_rejected_without_following_it(self):
        self.assertIsNone(DELIVERY.NoRedirect().redirect_request(None, None, 302, "", {}, "https://example.test"))

    def test_only_explicit_ipv4_loopback_receiver_is_accepted(self):
        for address in ("https://example.test", "http://localhost:9093", "http://127.0.0.2:9093",
                        "http://user:pass@127.0.0.1:9093", "http://127.0.0.1:9093/path",
                        "http://127.0.0.1:9093?secret=1", "http://127.0.0.1:9093#secret",
                        "http://127.0.0.1:9093/", "http://127.0.0.1:0", "http://127.0.0.1:65536"):
            with self.subTest(address=address):
                with self.assertRaises(ValueError):
                    DELIVERY.validate_receiver(address)
        self.assertEqual(DELIVERY.validate_receiver("http://127.0.0.1:19093"), "http://127.0.0.1:19093")

    def test_accepted_api_response_is_not_external_receipt_proof(self):
        with patch.object(DELIVERY, "send_alert", return_value=200) as send:
            code, event = self.invoke("--send", "--confirm-send", "--phase", "resolved")
        self.assertEqual(code, 0)
        self.assertEqual(event["status"], "accepted")
        self.assertFalse(event["receiptVerified"])
        self.assertEqual(send.call_count, 1)
        self.assertEqual(event["phase"], "resolved")

    def test_errors_are_redacted_uncertain_and_never_retried(self):
        errors = (URLError(PRIVATE_VALUE), TimeoutError(PRIVATE_VALUE), OSError(PRIVATE_VALUE),
                  HTTPError("http://127.0.0.1:9093", 500, PRIVATE_VALUE, {}, None))
        for error in errors:
            with self.subTest(error=type(error).__name__), patch.object(DELIVERY, "send_alert", side_effect=error) as send:
                code, event = self.invoke("--send", "--confirm-send")
            self.assertEqual(code, 1)
            self.assertEqual(event["status"], "unconfirmed")
            self.assertNotIn(PRIVATE_VALUE, json.dumps(event))
            self.assertEqual(send.call_count, 1)

    def test_non_success_status_is_not_reported_as_accepted(self):
        for status in (204, 301, 400, 500):
            with self.subTest(status=status), patch.object(DELIVERY, "send_alert", return_value=status):
                code, event = self.invoke("--send", "--confirm-send")
            self.assertEqual(code, 1)
            self.assertFalse(event["receiptVerified"])

    def test_invalid_input_never_echoes_content_or_opens_receiver(self):
        for arguments in (["--test-id", "<@U123>"], ["--test-id", "x" * 65], ["--test-id", ""],
                          ["--receiver", PRIVATE_VALUE], ["--phase", PRIVATE_VALUE], [PRIVATE_VALUE]):
            error = io.StringIO()
            with self.subTest(arguments=arguments), contextlib.redirect_stderr(error), patch.object(DELIVERY, "send_alert") as send:
                with self.assertRaises(SystemExit) as caught:
                    DELIVERY.main(arguments)
            self.assertEqual(caught.exception.code, 2)
            self.assertNotIn(PRIVATE_VALUE, error.getvalue())
            self.assertNotIn("<@U123>", error.getvalue())
            send.assert_not_called()


class AlertConfigurationTest(unittest.TestCase):
    def test_default_configuration_has_no_receiver_targets(self):
        self.assertEqual(json.loads((ROOT / "infra/prometheus/alertmanager-targets.json").read_text()), [])
        config = (ROOT / "infra/prometheus/prometheus.yml").read_text()
        self.assertIn("alerting:", config)
        self.assertIn("/etc/prometheus/alertmanager-targets.json", config)
        compose = (ROOT / "infra/docker-compose.yml").read_text()
        self.assertIn("${PROMETHEUS_ALERTMANAGER_TARGETS_FILE:-./prometheus/alertmanager-targets.json}:/etc/prometheus/alertmanager-targets.json:ro", compose)
        self.assertNotIn("alertmanager:", compose)

    def test_overlay_requires_explicit_image_and_secret_and_restricts_api(self):
        overlay = (ROOT / "infra/docker-compose.alerting.yml").read_text()
        self.assertIn("${ALERTMANAGER_IMAGE:?", overlay)
        self.assertIn("${ALERTMANAGER_SLACK_WEBHOOK_FILE:?", overlay)
        self.assertIn('"127.0.0.1:${ALERTMANAGER_PORT_HOST:-9093}:9093"', overlay)
        self.assertIn("--cluster.listen-address=", overlay)
        self.assertIn("read_only: true", overlay)
        self.assertIn("no-new-privileges:true", overlay)
        self.assertIn("sugang-helper-egress", overlay)
        targets = json.loads((ROOT / "infra/alertmanager/prometheus-targets.json").read_text())
        self.assertEqual(targets, [{"targets": ["alertmanager:9093"]}])

    def test_receiver_uses_secret_file_and_sends_resolutions(self):
        config = (ROOT / "infra/alertmanager/alertmanager.yml").read_text()
        self.assertIn("api_url_file: /run/secrets/alertmanager_slack_webhook", config)
        self.assertIn("send_resolved: true", config)
        self.assertIn("follow_redirects: false", config)
        self.assertIn("group_by: [alertname, service, test_id]", config)
        self.assertIn('[줍줍 문제 발생]', config)
        self.assertIn('[줍줍 경보 해제]', config)
        self.assertIn('.CommonAnnotations.summary', config)
        self.assertIn('대상: DB·첨부파일 백업', config)
        self.assertIn('점검 ID:', config)
        self.assertIn('기존 백업이 사라졌다는 뜻은 아닙니다.', config)
        self.assertIn('첫 대응: 백업 작업 로그', config)
        self.assertIn('경보 ID:', config)
        self.assertIn('규칙 등급: 경고(warning)', config)
        self.assertIn('감지 시각:', config)
        self.assertIn('해제 시각:', config)
        self.assertNotIn('.CommonLabels.alertname', config)
        self.assertIn("title_link: ''", config)
        self.assertNotIn("hooks.slack.com", config)
        self.assertNotIn("api_url:", config)
        self.assertNotIn(".Annotations.description", config)
        self.assertNotIn(".Labels.Values", config)


if __name__ == "__main__":
    unittest.main()
