#!/usr/bin/env python3
"""Bounded, allowlisted Docker event snapshots; never persist raw log lines."""
import argparse
from datetime import datetime, timedelta, timezone
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import selectors
import shutil
import subprocess
import tempfile
import time
import uuid

MAX_INPUT_BYTES = 64 * 1024 * 1024
MAX_LINE_BYTES = 256 * 1024
EVENTS = frozenset({
    'api_error', 'http_failure', 'run_failed', 'run_succeeded', 'crawler_data_stale',
    'crawler_freshness_check_failed', 'crawler_failure_handling_failed',
    'crawler_failure_record_failed', 'crawler_failure_alert_failed',
    'notification_delivery_processing_failed', 'notification_backlog_check_failed',
    'slack_alert_delivery_rejected', 'slack_alert_delivery_failed',
    'slack_alert_shutdown_abandoned', 'admin_audit_retention_completed',
})
STAGES = frozenset({'FETCH_PARSE', 'PERSIST', 'JUMP_BOOTSTRAP', 'COURSE_API'})
LEVELS = frozenset({'INFO', 'WARN', 'ERROR'})
TIMESTAMP = re.compile(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})')
TYPE_NAME = re.compile(r'[A-Za-z_$][A-Za-z0-9_$]{0,127}')
UUID = re.compile(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, '중요 이벤트 백업 인자가 올바르지 않습니다. --help를 확인하세요.\n')


def _timestamp(value):
    if not isinstance(value, str) or not TIMESTAMP.fullmatch(value):
        raise ValueError('invalid timestamp')
    return datetime.fromisoformat(value.replace('Z', '+00:00')).astimezone(timezone.utc)


def _options(arguments):
    parser = SafeParser(description='최소 중요 이벤트 백업 (Docker 로그 읽기 전용)')
    parser.add_argument('--release-root', default=os.environ.get('RELEASE_ROOT', '/home/ubuntu/jbnu-sugang-helper'))
    parser.add_argument('--container', default='sugang-helper-app')
    parser.add_argument('--since', help='시간대가 있는 ISO8601 시작 시각; 기본값 종료 10분 전')
    parser.add_argument('--until', help='시간대가 있는 ISO8601 종료 시각; 기본값 현재 UTC')
    parser.add_argument('--timeout-seconds', type=int, default=210)
    options = parser.parse_args(arguments)
    try:
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,127}', options.container):
            raise ValueError('invalid container')
        if not 1 <= options.timeout_seconds <= 210:
            raise ValueError('invalid timeout')
        if options.until is None:
            options.until = datetime.now(timezone.utc).isoformat()
        options.end = _timestamp(options.until)
        if options.since is None:
            options.since = (options.end - timedelta(minutes=10)).isoformat()
        options.start = _timestamp(options.since)
        if not timedelta(0) < options.end - options.start <= timedelta(hours=1):
            raise ValueError('invalid window')
        if options.end > datetime.now(timezone.utc) + timedelta(seconds=5):
            raise ValueError('future window')
        if not Path(options.release_root).is_absolute():
            raise ValueError('absolute root required')
    except (ValueError, OverflowError):
        parser.error('invalid options')
    return options


def _unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('duplicate key')
        result[key] = value
    return result


def _minimal_event(line, options):
    try:
        source = json.loads(line, object_pairs_hook=_unique_object)
        if not isinstance(source, dict):
            return None
        event = source.get('event')
        level = source.get('level')
        if not isinstance(event, str) or event not in EVENTS:
            return None
        if not isinstance(level, str) or level not in LEVELS:
            return None
        stamp = source.get('@timestamp', source.get('timestamp'))
        instant = _timestamp(stamp)
        if not options.start <= instant <= options.end:
            return None
    except (ValueError, UnicodeError, RecursionError, OverflowError):
        return None
    result = {'timestamp': stamp, 'event': event, 'level': level}
    stage = source.get('stage')
    if isinstance(stage, str) and stage in STAGES:
        result['stage'] = stage
    for key in ('exceptionType', 'errorType', 'rootCauseType', 'originalType', 'recordingType', 'alertType'):
        value = source.get(key)
        if isinstance(value, str) and TYPE_NAME.fullmatch(value):
            result[key] = value
    # 서버가 생성하는 수집 실행 UUID만 보존한다. 사용자/알림 수신자 ID는 제외한다.
    run_id = source.get('crawlerRunId')
    if isinstance(run_id, str) and UUID.fullmatch(run_id):
        result['crawlerRunId'] = run_id
    return result


def _record_line(line, archive, counters, options):
    if len(line) > MAX_LINE_BYTES:
        raise ValueError('line limit')
    record = _minimal_event(line, options)
    if record is None:
        counters['skippedLines'] += 1
        return
    archive.write((json.dumps(record, sort_keys=True, separators=(',', ':')) + '\n').encode())
    counters['recordCount'] += 1


def _read_logs(process, archive, counters, options, deadline):
    pending = bytearray()
    with selectors.DefaultSelector() as selector:
        selector.register(process.stdout, selectors.EVENT_READ)
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise TimeoutError('docker timeout')
            if not selector.select(min(remaining, 1)):
                continue
            chunk = os.read(process.stdout.fileno(), 65536)
            if not chunk:
                break
            counters['inputBytes'] += len(chunk)
            if counters['inputBytes'] > MAX_INPUT_BYTES:
                raise ValueError('input limit')
            pending.extend(chunk)
            while b'\n' in pending:
                line, _, remainder = pending.partition(b'\n')
                pending = bytearray(remainder)
                _record_line(line, archive, counters, options)
                if time.monotonic() >= deadline:
                    raise TimeoutError('processing timeout')
            if len(pending) > MAX_LINE_BYTES:
                raise ValueError('line limit')
        if pending:
            _record_line(pending, archive, counters, options)
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        raise TimeoutError('docker timeout')
    if process.wait(timeout=remaining) != 0:
        raise RuntimeError('docker failed')


def _capture_logs(archive, counters, options):
    command = ['docker', 'logs', '--since', options.since, '--until', options.until, options.container]
    # Docker stderr에는 컨테이너 stderr 로그와 CLI 오류가 함께 올 수 있다.
    # 종료 코드는 반드시 확인하고, 어느 원문도 진단 출력/파일에 남기지 않는다.
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT) as process:
        try:
            _read_logs(process, archive, counters, options, time.monotonic() + options.timeout_seconds)
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=5)


def _private_file(path):
    return os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'wb')


def _write_file(path, payload):
    with _private_file(path) as output:
        output.write(payload)
        output.flush()
        os.fsync(output.fileno())


def _digest(path):
    digest = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(65536), b''):
            digest.update(chunk)
    return digest.hexdigest()


def _sync_directory(path):
    descriptor = os.open(path, os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def _publish_snapshot(options):
    root = Path(options.release_root)
    for directory in (root, root / 'backups', root / 'backups/events'):
        if directory.is_symlink():
            raise ValueError('symlink destination')
        directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    destination = root / 'backups/events'
    stage = Path(tempfile.mkdtemp(prefix='.partial-events-', dir=destination))
    try:
        counters = {'recordCount': 0, 'skippedLines': 0, 'inputBytes': 0}
        with _private_file(stage / 'events.jsonl.gz') as output:
            with gzip.GzipFile(fileobj=output, mode='wb', filename='', mtime=0) as archive:
                _capture_logs(archive, counters, options)
            output.flush()
            os.fsync(output.fileno())
        manifest = {
            'schemaVersion': 1, 'source': 'docker_logs', 'container': options.container,
            'since': options.since, 'until': options.until,
            'createdAt': datetime.now(timezone.utc).isoformat(),
            'completeHistory': False, 'coverage': 'available_container_logs_only',
            'filterVersion': 1, 'allowedEvents': sorted(EVENTS),
            'maxInputBytes': MAX_INPUT_BYTES, 'maxLineBytes': MAX_LINE_BYTES,
            **counters,
        }
        _write_file(stage / 'events.jsonl.gz.manifest', (json.dumps(manifest, indent=2) + '\n').encode())
        for name in ('events.jsonl.gz', 'events.jsonl.gz.manifest'):
            checksum = _digest(stage / name) + '  ' + name + '\n'
            _write_file(stage / (name + '.sha256'), checksum.encode())
        snapshot_name = 'events-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex
        _sync_directory(stage)
        os.rename(stage, destination / snapshot_name)
        _sync_directory(destination)
        return counters
    finally:
        # 이번 실행의 비공개 임시 파일만 정리하며, 게시된 백업은 삭제하지 않는다.
        if stage.exists():
            shutil.rmtree(stage)


def _log_backup_result(status, counters=None):
    event = {'event': 'important_event_backup', 'status': status,
             'message': '중요 이벤트 백업에 실패했습니다.'}
    if status == 'ok':
        event['message'] = '중요 이벤트 백업을 게시했습니다.'
    if counters is not None:
        event.update(counters)
    print(json.dumps(event, ensure_ascii=False))


def main(arguments=None):
    options = _options(arguments)
    try:
        counters = _publish_snapshot(options)
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError):
        _log_backup_result('error')
        return 1
    _log_backup_result('ok', counters)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
