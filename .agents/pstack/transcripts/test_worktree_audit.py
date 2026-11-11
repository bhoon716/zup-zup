import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "skills/poteto-mode/scripts/worktree-audit.sh"


class WorktreeAuditTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="pstack-audit-")
        self.addCleanup(temp.cleanup)
        fixture = Path(temp.name).resolve()
        self.repo = fixture / "repo"
        self.checkout = fixture / "linked checkout"
        self.codex = fixture / "codex"
        self.sessions = self.codex / "sessions/2026/11/11"
        self.sessions.mkdir(parents=True)
        bin_dir = fixture / "bin"
        bin_dir.mkdir()
        for command in ("gh", "origin"):
            stub = bin_dir / command
            stub.write_text("#!/bin/sh\nexit 1\n")
            stub.chmod(0o755)
        self.env = {
            **os.environ,
            "CODEX_HOME": str(self.codex),
            "GIT_CONFIG_GLOBAL": os.devnull,
            "GIT_CONFIG_NOSYSTEM": "1",
            "PATH": str(bin_dir) + os.pathsep + os.environ["PATH"],
        }
        subprocess.run(["git", "init", "-q", "-b", "main", str(self.repo)], env=self.env, check=True)
        self.git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.test",
                 "commit", "--allow-empty", "-qm", "fixture")
        self.git("worktree", "add", "-qb", "linked", str(self.checkout))

    def git(self, *args):
        subprocess.run(["git", "-C", str(self.repo), *args], env=self.env, check=True)

    def activity(self, thread, suffix="", cwd=None, command=None):
        path = self.sessions / f"rollout-2026-11-11T00-00-00-{thread}{suffix}.jsonl"
        records = [{"type": "session_meta", "payload": {"id": thread, "cwd": str(cwd or self.checkout)}}]
        if command:
            records.append({
                "type": "response_item",
                "payload": {"type": "function_call", "name": "exec_command", "arguments": json.dumps({"cmd": command})},
            })
        path.write_text("".join(json.dumps(record) + "\n" for record in records))

    def audit(self):
        run = subprocess.run(
            [str(SCRIPT), "--base", "main", str(self.repo)],
            env=self.env, capture_output=True, text=True, check=True, timeout=15,
        )
        row, = run.stdout.splitlines()[2:]
        *_, bucket, path = row.split("\t")
        self.assertEqual(path, str(self.checkout))
        return bucket, run.stderr

    def test_recent_codex_session_keeps_the_matching_worktree(self):
        self.assertEqual(self.audit()[0], "safe")
        self.activity("11111111-1111-1111-1111-111111111111")
        self.assertEqual(self.audit()[0], "verify-recent-chat")

    def test_ambiguous_rollout_prevents_safe_classification(self):
        thread = "22222222-2222-2222-2222-222222222222"
        self.activity(thread)
        self.activity(thread, "_replacement")
        bucket, stderr = self.audit()
        self.assertEqual(bucket, "review")
        self.assertIn("Codex 최근 활동을 확인할 수 없어", stderr)

    def test_recent_activity_from_a_shell_command_with_a_worktree_path(self):
        self.activity("33333333-3333-3333-3333-333333333333", cwd=self.repo,
                      command=f'git -C "{self.checkout}-other" status --short')
        self.assertEqual(self.audit()[0], "safe")
        self.activity("44444444-4444-4444-4444-444444444444", cwd=self.repo,
                      command=f'git -C "{self.checkout}" status --short')
        self.assertEqual(self.audit()[0], "verify-recent-chat")


if __name__ == "__main__":
    unittest.main()
