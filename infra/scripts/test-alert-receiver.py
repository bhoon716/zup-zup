#!/usr/bin/env python3
"""Preview or explicitly submit synthetic alerts to an approved local Alertmanager."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import re
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, "알림 점검 인자가 올바르지 않습니다. --help를 확인하세요.\n")


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def test_identifier(value):
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}", value):
        raise ValueError("invalid test identifier")
    return value


def validate_receiver(value):
    match = re.fullmatch(r"http://127\.0\.0\.1:([0-9]{1,5})", value)
    if not match or not 1 <= int(match.group(1)) <= 65535:
        raise ValueError("loopback receiver required")
    return value


def parser():
    result = SafeParser(description="합성 TEST/RESOLVED 알림. 기본값은 전송하지 않는 미리보기입니다.")
    result.add_argument("--test-id", required=True, type=test_identifier)
    result.add_argument("--phase", choices=("firing", "resolved"), default="firing")
    result.add_argument("--receiver", type=validate_receiver, default="http://127.0.0.1:9093")
    result.add_argument("--send", action="store_true", help="실제 Alertmanager API에 1회 제출")
    result.add_argument("--confirm-send", action="store_true", help="승인된 수신처의 외부 알림 발생을 확인")
    return result


def utc_timestamp(value):
    return value.isoformat().replace("+00:00", "Z")


def build_alert(test_id, phase, now):
    test_identifier(test_id)
    end = now + timedelta(minutes=10)
    summary = "TEST: 합성 알림 수신 경로 점검"
    if phase == "resolved":
        end = now
        summary = "RESOLVED: 합성 알림 수신 경로 점검 종료"
    return {
        "labels": {"alertname": "AlertDeliverySmokeTest", "severity": "info",
                   "service": "sugang-helper", "test_id": test_id},
        "annotations": {"summary": summary},
        "startsAt": utc_timestamp(now - timedelta(seconds=1)),
        "endsAt": utc_timestamp(end),
    }


def send_alert(receiver, alert):
    # 프록시·리다이렉트·재시도 없이 동일한 loopback 수신처에 한 번만 제출한다.
    validate_receiver(receiver)
    opener = build_opener(ProxyHandler({}), NoRedirect())
    request = Request(receiver + "/api/v2/alerts", data=json.dumps([alert]).encode("utf-8"),
                      headers={"Content-Type": "application/json"}, method="POST")
    with opener.open(request, timeout=10) as response:
        return response.status


def emit_delivery_result(test_id, phase, status, reason):
    print(json.dumps({"event": "alert_delivery_test", "schemaVersion": 1,
                      "testId": test_id, "phase": phase, "status": status,
                      "reason": reason, "receiptVerified": False}, ensure_ascii=False, sort_keys=True))


def main(argv=None):
    options = parser().parse_args(argv)
    if options.send != options.confirm_send:
        emit_delivery_result(options.test_id, options.phase, "blocked", "send_confirmation_required")
        return 2
    if not options.send:
        emit_delivery_result(options.test_id, options.phase, "preview", "no_request_sent")
        return 0
    alert = build_alert(options.test_id, options.phase, datetime.now(timezone.utc))
    try:
        status = send_alert(options.receiver, alert)
    except (HTTPError, URLError, OSError):
        # 응답/예외 원문에는 URL·민감 본문이 있을 수 있다. 불명확한 전송은 재시도하지 않는다.
        emit_delivery_result(options.test_id, options.phase, "unconfirmed", "request_not_confirmed")
        return 1
    if status != 200:
        emit_delivery_result(options.test_id, options.phase, "unconfirmed", "unexpected_status")
        return 1
    emit_delivery_result(options.test_id, options.phase, "accepted", "check_receiver_for_receipt")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
