from contextlib import closing
import hashlib
import hmac
import json
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import time
import unittest

import webhooks


NOW = 1_700_000_000
SETTINGS = {
    "slack": {
        "app_id": "A12",
        "team_id": "T12",
        "source_channel_id": "C12",
        "triage_identity_user_id": "U99",
    },
    "routines": {"smoke": {"prompt_path": "prompt.md"}},
}
SECRETS = {"BENNY_SLACK_SIGNING_SECRET": "fixture-slack", "PSTACK_WEBHOOK_KEY_SMOKE": "fixture-routine"}


class WebhooksTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "jobs.sqlite3"
        self.db = webhooks.open_queue(self.path)
        self.addCleanup(self.db.close)

    def slack(self, payload, timestamp=NOW, signature=None):
        raw = json.dumps(payload).encode()
        signed = f"v0:{timestamp}:".encode() + raw
        if signature is None:
            signature = "v0=" + hmac.new(b"fixture-slack", signed, hashlib.sha256).hexdigest()
        headers = {
            "X-Slack-Signature": signature,
            "X-Slack-Request-Timestamp": str(timestamp),
        }
        return webhooks.receive(self.db, SETTINGS, "/slack/events", headers, raw, SECRETS, NOW)

    def test_same_root_queues_each_role_once_and_filters_other_events(self):
        event = {"type": "message", "channel": "C12", "user": "U12", "ts": "1700000000.123456"}
        report = {"type": "event_callback", "api_app_id": "A12", "team_id": "T12", "event": event}
        self.assertEqual(self.slack(report, timestamp=NOW - 301)[0], 401)
        self.assertEqual(self.slack(report, signature="v0=bad")[0], 401)
        self.assertEqual(self.slack({**report, "event": {**event, "thread_ts": "1699999999.000000"}})[0], 200)
        self.assertEqual(self.slack({**report, "event": {**event, "channel": "C99"}})[0], 200)
        self.assertEqual(self.slack({**report, "event": {**event, "user": "U99"}})[0], 200)
        self.assertEqual(self.slack({**report, "team_id": "T99"})[0], 200)
        self.assertIsNone(webhooks.claim(self.db, "benny-triage"))
        self.assertEqual(self.slack({**report, "event_id": "E12"})[0], 200)
        self.assertEqual(self.slack({**report, "event_id": "E34"})[0], 200)
        expected = {"source_channel_id": "C12", "message_ts": event["ts"], "thread_ts": event["ts"]}
        for role in webhooks.BENNY_SKILLS:
            _, raw = webhooks.claim(self.db, role)
            self.assertEqual(json.loads(raw), expected)
            with closing(webhooks.open_queue(self.path)) as second_worker:
                self.assertIsNone(webhooks.claim(second_worker, role))
        self.assertEqual(stat.S_IMODE(self.path.stat().st_mode), 0o600)

    def test_slack_challenge_requires_signed_request(self):
        challenge = {"type": "url_verification", "challenge": "check"}
        self.assertEqual(self.slack(challenge, signature="bad")[0], 401)
        self.assertEqual(self.slack(challenge), (200, {"challenge": "check"}))

    def routine(self, headers, raw=b'{"action":"verify"}'):
        return webhooks.receive(self.db, SETTINGS, "/routines/smoke", headers, raw, SECRETS, NOW)

    def test_routine_auth_dedup_and_envelope(self):
        self.assertEqual(self.routine({"Authorization": "Bearer incorrect"})[0], 401)
        headers = {
            "Authorization": "Bearer fixture-routine",
            "Idempotency-Key": "one-delivery",
            "Content-Type": "application/json",
        }
        self.assertTrue(self.routine(headers)[1]["queued"])
        self.assertFalse(self.routine(headers)[1]["queued"])
        _, payload = webhooks.claim(self.db, "smoke")
        event = json.loads(payload)
        self.assertEqual(event["body_digest"], hashlib.sha256(event["body"].encode()).hexdigest())
        self.assertEqual(event["headers"]["content-type"], "application/json")
        self.assertNotIn("fixture-routine", payload)
        self.assertEqual(event["timestamp_ms"], NOW * 1000)
        with self.assertRaises(ValueError):
            self.routine(headers, b"[]")

    def test_worker_launches_in_committed_checkout_and_records_completion(self):
        root = Path(self.temp.name)
        repo = root / "repo"
        repo.mkdir()
        webhooks.git(repo, "init", "-q")
        (repo / "prompt.md").write_text("committed prompt")
        installer = repo / ".agents/pstack/install.py"
        installer.parent.mkdir(parents=True)
        installer.write_text(
            "from pathlib import Path\n"
            "Path(__file__).with_name('ready').write_text('installed')\n"
        )
        webhooks.git(repo, "add", "prompt.md", ".agents/pstack/install.py")
        webhooks.git(repo, "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-qm", "fixture")
        (repo / "prompt.md").write_text("uncommitted change")
        binary = root / "codex"
        binary.write_text(
            "#!/usr/bin/env python3\n"
            "import os, pathlib, sys\n"
            "p = pathlib.Path(sys.argv[sys.argv.index('--cd') + 1])\n"
            "assert (p / 'prompt.md').read_text() == 'committed prompt'\n"
            "assert (p / '.agents/pstack/ready').read_text() == 'installed'\n"
            "assert sys.argv[sys.argv.index('--model') + 1] == 'gpt-5.5'\n"
            "assert 'model_reasoning_effort=xhigh' in sys.argv\n"
            "assert 'PSTACK_WEBHOOK_KEY_SMOKE' not in os.environ\n"
            "assert 'BENNY_SLACK_SIGNING_SECRET' not in os.environ\n"
            "assert 'Read and follow prompt.md' in sys.stdin.read()\n"
        )
        binary.chmod(0o700)
        config = root / "config.json"
        config.write_text(json.dumps({"revision": "HEAD", "routines": SETTINGS["routines"]}))
        self.assertTrue(self.routine({"X-Automation-Key": "fixture-routine"})[1]["queued"])
        completed = subprocess.run(
            [
                "python3", webhooks.__file__, "--config", str(config), "--queue", str(self.path),
                "work", "smoke", "--repo", str(repo), "--timeout", "5", "--once",
                "--model", "gpt-5.5", "--effort", "xhigh",
            ],
            env={**os.environ, **SECRETS, "PATH": f"{root}{os.pathsep}{os.environ['PATH']}"},
            capture_output=True, text=True,
        )
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertEqual(self.db.execute("SELECT state, event, exit_code FROM jobs").fetchone(), ("completed", "", 0))
        self.assertEqual((repo / "prompt.md").read_text(), "uncommitted change")
        self.assertEqual(webhooks.git(repo, "worktree", "list", "--porcelain").count("worktree "), 1)

    def test_worker_cleans_up_on_timeout_and_sigterm(self):
        root = Path(self.temp.name)
        repo = root / "repo"
        repo.mkdir()
        webhooks.git(repo, "init", "-q")
        (repo / "prompt.md").write_text("committed prompt")
        webhooks.git(repo, "add", "prompt.md")
        webhooks.git(repo, "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-qm", "fixture")
        binary = root / "codex"
        child_pid = root / "child-pid"
        binary.write_text(
            "#!/usr/bin/env python3\n"
            "import os, pathlib, time\n"
            f"pathlib.Path({str(child_pid)!r}).write_text(str(os.getpid()))\n"
            "time.sleep(30)\n"
        )
        binary.chmod(0o700)
        config = root / "config.json"
        config.write_text(json.dumps({"revision": "HEAD", "routines": SETTINGS["routines"]}))
        command = [
            "python3", webhooks.__file__, "--config", str(config), "--queue", str(self.path),
            "work", "smoke", "--repo", str(repo), "--once",
        ]
        env = {**os.environ, "PATH": f"{root}{os.pathsep}{os.environ['PATH']}"}
        for outcome, timeout, stop in ((124, "1", False), (143, "15", True)):
            with self.subTest(outcome=outcome):
                child_pid.unlink(missing_ok=True)
                self.routine({"X-Automation-Key": "fixture-routine"})
                with subprocess.Popen([*command, "--timeout", timeout], env=env, stderr=subprocess.DEVNULL) as worker:
                    try:
                        deadline = time.monotonic() + 5
                        while not child_pid.exists() and time.monotonic() < deadline:
                            time.sleep(0.02)
                        pid = int(child_pid.read_text())
                        if stop:
                            worker.terminate()
                        self.assertEqual(worker.wait(timeout=5), outcome)
                        with self.assertRaises(ProcessLookupError):
                            os.kill(pid, 0)
                    finally:
                        if worker.poll() is None:
                            worker.terminate()
                self.assertEqual(
                    self.db.execute("SELECT state, event, exit_code FROM jobs ORDER BY rowid DESC").fetchone(),
                    ("failed", "", outcome),
                )
                self.assertEqual(webhooks.git(repo, "worktree", "list", "--porcelain").count("worktree "), 1)


if __name__ == "__main__":
    unittest.main()
