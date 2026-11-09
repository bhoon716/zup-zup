#!/usr/bin/env python3
"""중요 이벤트 백업 오프라인 계약 테스트. Docker를 실행하지 않는다."""
import contextlib
import gzip
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).with_name('backup-important-events.py')
SINCE = '2026-11-08T00:00:00Z'
UNTIL = '2026-11-08T00:10:00Z'
STAMP = '2026-11-08T00:05:00.123456789Z'
RUN_ID = '11111111-2222-4333-8444-555555555555'
SPEC = importlib.util.spec_from_file_location('event_backup', SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)
REAL_POPEN = subprocess.Popen


def fixture(**values):
    result = {'@timestamp': STAMP, 'event': 'run_failed', 'level': 'ERROR',
              'stage': 'FETCH_PARSE', 'exceptionType': 'IllegalStateException',
              'crawlerRunId': RUN_ID, 'message': 'PRIVATE', 'stack_trace': 'PRIVATE',
              'url': 'PRIVATE', 'userId': 23, 'email': 'PRIVATE'}
    result.update(values)
    return json.dumps(result).encode() + b'\n'


class EventBackupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.args = ['--release-root', str(self.root), '--since', SINCE, '--until', UNTIL]
        self.commands = []

    def run_export(self, content=b'', return_code=0, delay=0, arguments=None):
        def start(command, **kwargs):
            self.commands.append(command)
            program = 'import os,time; os.write(1,' + repr(content) + '); time.sleep(' + str(delay) + '); raise SystemExit(' + str(return_code) + ')'
            return REAL_POPEN([sys.executable, '-c', program], **kwargs)
        args = self.args
        if arguments is not None:
            args = arguments
        with patch.object(MODULE.subprocess, 'Popen', side_effect=start), contextlib.redirect_stdout(io.StringIO()) as output:
            code = MODULE.main(args)
        return code, output.getvalue()

    def snapshots(self):
        return list((self.root / 'backups/events').glob('events-*'))

    def test_only_allowlisted_minimal_fields_are_atomically_published(self):
        # given
        content = fixture() + fixture(event='http_request') + b'legacy private text\n'
        # when
        code, output = self.run_export(content)
        snapshot, = self.snapshots()
        events = [json.loads(line) for line in gzip.decompress((snapshot / 'events.jsonl.gz').read_bytes()).splitlines()]
        manifest = json.loads((snapshot / 'events.jsonl.gz.manifest').read_text())
        # then
        self.assertEqual(code, 0)
        self.assertEqual(events, [{'timestamp': STAMP, 'event': 'run_failed', 'level': 'ERROR',
                                  'stage': 'FETCH_PARSE', 'exceptionType': 'IllegalStateException', 'crawlerRunId': RUN_ID}])
        self.assertEqual(manifest['recordCount'], 1)
        self.assertFalse(manifest['completeHistory'])
        self.assertEqual(manifest['skippedLines'], 2)
        self.assertNotIn('PRIVATE', output + (snapshot / 'events.jsonl.gz.manifest').read_text())
        self.assertEqual(self.commands[0], ['docker', 'logs', '--since', SINCE, '--until', UNTIL, 'sugang-helper-app'])
        for checksum in ('events.jsonl.gz.sha256', 'events.jsonl.gz.manifest.sha256'):
            lines = (snapshot / checksum).read_text().splitlines()
            self.assertEqual(len(lines), 1)
            digest, name = lines[0].split('  ')
            self.assertEqual(name + '.sha256', checksum)
            self.assertEqual(hashlib.sha256((snapshot / name).read_bytes()).hexdigest(), digest)
        self.assertEqual(snapshot.stat().st_mode & 0o777, 0o700)
        self.assertEqual(snapshot.stat().st_uid, os.geteuid())
        for artifact in snapshot.iterdir():
            self.assertEqual(artifact.stat().st_uid, os.geteuid())
            self.assertEqual(artifact.stat().st_mode & 0o777, 0o600)
        self.assertEqual(set(p.name for p in snapshot.iterdir()), {'events.jsonl.gz', 'events.jsonl.gz.manifest', 'events.jsonl.gz.sha256', 'events.jsonl.gz.manifest.sha256'})

    def test_private_output_permission_failure_does_not_change_ownership_or_retry(self):
        # given
        destination = self.root / 'backups/events'
        destination.mkdir(parents=True, mode=0o700)
        before = destination.stat()
        with patch.object(MODULE, '_private_file', side_effect=PermissionError('PRIVATE')), \
                patch.object(MODULE.os, 'chown') as chown, patch.object(MODULE.os, 'chmod') as chmod:
            # when
            code, output = self.run_export(fixture())
        # then
        self.assertEqual(code, 1)
        self.assertEqual(self.snapshots(), [])
        self.assertEqual(self.commands, [])
        self.assertNotIn('PRIVATE', output)
        self.assertEqual(destination.stat().st_uid, before.st_uid)
        self.assertEqual(destination.stat().st_mode, before.st_mode)
        chown.assert_not_called()
        chmod.assert_not_called()

    def test_invalid_optional_fields_and_user_fields_never_survive(self):
        # given
        content = fixture(stage='private stage', exceptionType='https://secret', crawlerRunId='user@example.com', errorType={'secret': True})
        # when
        code, _ = self.run_export(content)
        snapshot, = self.snapshots()
        record = json.loads(gzip.decompress((snapshot / 'events.jsonl.gz').read_bytes()))
        # then
        self.assertEqual(code, 0)
        self.assertEqual(set(record), {'timestamp', 'event', 'level'})

    def test_known_failure_type_fields_are_validated_without_copying_raw_errors(self):
        # given
        content = fixture(rootCauseType='IOException', originalType='RuntimeException',
                          recordingType='SQLException', alertType='TimeoutException',
                          rootType='UNUSED_PRIVATE', error={'message': 'PRIVATE'})
        content += fixture(rootCauseType='https://PRIVATE', originalType=['PRIVATE'],
                           recordingType='PRIVATE message', alertType='x' * 129)
        # when
        code, _ = self.run_export(content)
        snapshot, = self.snapshots()
        records = [json.loads(line) for line in gzip.decompress((snapshot / 'events.jsonl.gz').read_bytes()).splitlines()]
        # then
        self.assertEqual(code, 0)
        self.assertEqual(records[0]['rootCauseType'], 'IOException')
        self.assertEqual(records[0]['originalType'], 'RuntimeException')
        self.assertEqual(records[0]['recordingType'], 'SQLException')
        self.assertEqual(records[0]['alertType'], 'TimeoutException')
        self.assertNotIn('PRIVATE', json.dumps(records))
        self.assertTrue({'rootCauseType', 'originalType', 'recordingType', 'alertType'}.isdisjoint(records[1]))

    def test_invalid_required_fields_outside_window_and_unsupported_events_are_counted(self):
        # given
        content = fixture(event=['secret']) + fixture(level='PRIVATE') + fixture(**{'@timestamp': '2026-11-07T00:00:00Z'}) + b'{broken\n'
        # when
        code, _ = self.run_export(content)
        snapshot, = self.snapshots()
        manifest = json.loads((snapshot / 'events.jsonl.gz.manifest').read_text())
        # then
        self.assertEqual(code, 0)
        self.assertEqual(manifest['recordCount'], 0)
        self.assertEqual(manifest['skippedLines'], 4)

    def test_nonzero_docker_exit_never_publishes_or_exposes_stderr(self):
        # when
        code, output = self.run_export(fixture() + b'PRIVATE docker error\n', return_code=1)
        # then
        self.assertEqual(code, 1)
        self.assertEqual(self.snapshots(), [])
        self.assertNotIn('PRIVATE', output)
        self.assertNotIn('"status": "ok"', output)

    def test_timeout_never_publishes(self):
        # when
        code, _ = self.run_export(fixture(), delay=2, arguments=self.args + ['--timeout-seconds', '1'])
        # then
        self.assertEqual(code, 1)
        self.assertEqual(self.snapshots(), [])

    def test_input_and_line_bounds_fail_without_publication(self):
        # given
        for constant, limit, content in [('MAX_INPUT_BYTES', 10, fixture()), ('MAX_LINE_BYTES', 10, b'x' * 20)]:
            with self.subTest(constant=constant), patch.object(MODULE, constant, limit):
                # when
                code, _ = self.run_export(content)
                # then
                self.assertEqual(code, 1)
                self.assertEqual(self.snapshots(), [])

    def test_publication_failure_leaves_no_success_or_temporary_snapshot(self):
        # given
        with patch.object(MODULE.os, 'rename', side_effect=OSError('PRIVATE')):
            # when
            code, output = self.run_export(fixture())
        # then
        self.assertEqual(code, 1)
        self.assertEqual(list((self.root / 'backups/events').iterdir()), [])
        self.assertNotIn('PRIVATE', output)

    def test_invalid_arguments_never_start_docker(self):
        # given
        cases = [['--container', '-bad'], ['--container', 'name;cat'], ['--since', '2026-11-07T00:00:00Z'], ['--until', SINCE], ['--timeout-seconds', '241']]
        for suffix in cases:
            with self.subTest(suffix=suffix), contextlib.redirect_stderr(io.StringIO()):
                # when & then
                with self.assertRaises(SystemExit) as failure:
                    self.run_export(arguments=self.args + suffix)
                self.assertEqual(failure.exception.code, 2)
        self.assertEqual(self.commands, [])

    def test_default_window_is_ten_minutes_and_timeout_is_below_wrapper_limit(self):
        # when
        options = MODULE._options(['--release-root', str(self.root)])
        # then
        self.assertEqual((options.end - options.start).total_seconds(), 600)
        self.assertEqual(options.timeout_seconds, 210)

    def test_all_crawler_stages_are_kept_and_duplicate_json_keys_are_rejected(self):
        # given
        content = fixture(stage='JUMP_BOOTSTRAP') + fixture(stage='COURSE_API')
        content += fixture().replace(b'"event": "run_failed"', b'"event":"api_error", "event":"run_failed"')
        # when
        code, _ = self.run_export(content)
        snapshot, = self.snapshots()
        records = [json.loads(line) for line in gzip.decompress((snapshot / 'events.jsonl.gz').read_bytes()).splitlines()]
        # then
        self.assertEqual(code, 0)
        self.assertEqual([record['stage'] for record in records], ['JUMP_BOOTSTRAP', 'COURSE_API'])
        self.assertEqual(json.loads((snapshot / 'events.jsonl.gz.manifest').read_text())['skippedLines'], 1)

    def test_destination_symlink_is_rejected_without_starting_docker(self):
        # given
        (self.root / 'backups').symlink_to(self.root, target_is_directory=True)
        # when
        code, _ = self.run_export(fixture())
        # then
        self.assertEqual(code, 1)
        self.assertEqual(self.commands, [])

    def test_timestamp_alias_and_repeated_windows_are_supported(self):
        # given
        record = json.loads(fixture())
        record['timestamp'] = record.pop('@timestamp')
        # when
        first, _ = self.run_export(json.dumps(record).encode())
        second, _ = self.run_export(json.dumps(record).encode())
        # then
        self.assertEqual((first, second), (0, 0))
        self.assertEqual(len(self.snapshots()), 2)


if __name__ == '__main__':
    unittest.main()
