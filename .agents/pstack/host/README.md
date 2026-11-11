# pstack host integrations in Codex

Use this mapping when an upstream pstack instruction names a Cursor service. The upstream workflow owns its trigger, evidence, stop conditions, and permissions. The host supplies the corresponding execution surface.

## Browser, terminal, and PR verification

- For browser, Electron, and IDE checks, follow [control-ui](../cursor-team-kit/skills/control-ui/SKILL.md). Use the Codex computer/browser tool when it exposes the requested browser, or use the project's Playwright or CDP harness in an isolated test environment. Match a browser specified by the user. Label an alternate browser as translated evidence. Identify the actual app by a stable marker before acting. The project's [Playwright configuration](../../../apps/web/playwright.config.ts) includes traces, screenshots, and video on failure. When proof requires a recording of a successful run, use a disposable Playwright context with `recordVideo` and close the context to finalize the clip.
- For terminal and interactive CLI checks, follow [control-cli](../cursor-team-kit/skills/control-cli/SKILL.md) with the repository's own harness, tmux, or a PTY. Save the prompt and resulting terminal state as evidence. An exit code alone cannot establish an interaction outcome.
- For live PR verification, hand the verifier an isolated worktree and the exact base and head SHAs. Start the real artifact, run the same user path against each SHA, and report the corresponding verdict with evidence. A verifier who authored the change cannot issue the independent verdict. If a user path requires a remote service or environment, use a configured test instance or report the missing prerequisite.
- For Bugbot and security review, the existing GitHub review threads and check runs are the source of truth. Follow [Babysit](../skills/poteto-mode/playbooks/babysit.md), the upstream `watch-pr` CLI, and [Bugbot triage](../skills/poteto-mode/references/bugbot-triage.md) without changing their evidence or merge rules. An absent Bugbot integration produces no Bugbot verdict. An independent Codex review can report findings under its own identity, and required remote checks retain their original meaning.

## Authenticated webhook wake

The optional [webhook receiver](./webhooks.py) provides the wake used by [make-bot-ui](../skills/make-bot-ui/SKILL.md). It accepts an authenticated JSON object, stores it in a local queue, and wakes a Codex worker. The worker passes a `<webhook_event>` envelope containing `headers`, `body_digest`, `body`, and `timestamp_ms` to a prompt in the configured committed checkout. The credential headers are omitted. The sender key stays in the server's environment or secret manager.

Requirements are Python 3.12 or later with SQLite 3.35 or later, Git, patch, and authenticated Codex CLI. The receiver starts only when explicitly run. Keep the receiver and queue in a private runtime directory on an operator-controlled machine. Use a loopback listener and a TLS reverse proxy with request size, connection count, and rate limits for public ingress. Do not expose worker control or the queue to browsers.

Create a secret-free config outside the vendored pack. Use a real commit SHA for `revision` and the path of the committed prompt for `prompt_path`.

```json
{
  "revision": "<verified-commit-sha>",
  "routines": {
    "example": {"prompt_path": "docs/automation/example.md"}
  }
}
```

Set `PSTACK_WEBHOOK_KEY_EXAMPLE` in the receiver environment from a secret manager. Each routine has its own key variable, with its uppercase name and underscores in place of hyphens. Run the following from the repository root with a private runtime directory. The worker and receiver share one queue file.

```bash
python3 .agents/pstack/host/webhooks.py --config "$PSTACK_CONFIG" --queue "$PSTACK_QUEUE" serve
python3 .agents/pstack/host/webhooks.py --config "$PSTACK_CONFIG" --queue "$PSTACK_QUEUE" work example --timeout 900 --model gpt-6.1-sol --effort xhigh
python3 .agents/pstack/host/webhooks.py --config "$PSTACK_CONFIG" --queue "$PSTACK_QUEUE" status
```

The UI's backend sends one `POST /routines/example` with JSON and `Authorization: Bearer` or `X-Automation-Key`, using the configured key. Use the same eight-second timeout and single attempt as the upstream skill. If you explicitly redeliver a failed request, reuse an `Idempotency-Key` so a previously accepted request does not wake a second worker. The receiver responds `200` on durable acceptance. Worker completion is visible separately in `status`. Before announcing a live UI, send a harmless ignored action and check both signals.

The worker resolves its revision at startup, runs Codex in a temporary detached worktree for each event, removes the worktree, and clears processed event bodies from the queue. Restart workers after a revision or configuration change. Timeout, Ctrl-C, and SIGTERM stop the active Codex process group and record failure. An abrupt kill or loss of the machine can leave a `running` row. Check remote side effects before any manual replay. Retain the queue while duplicate event delivery is possible. Run separate triage and repro workers so the repro marker wait does not delay triage.

## Benny

[Benny's upstream intent](../automations/benny/FOR_AGENTS.md) defines two separate, dormant automations. On this host, subscribe a configured Slack app to new messages and deliver them to `POST /slack/events`. Codex desktop scheduled tasks are time-based. Web and mobile event tasks can receive Slack messages, but those tasks do not have the local project checkout required by the pack. [OpenAI scheduled task documentation](https://learn.chatgpt.com/docs/automations) · [Slack Events API](https://docs.slack.dev/apis/events-api/)

Follow the upstream [Benny setup](../automations/benny/skills/setup-benny/SKILL.md) for inputs, permissions, capability checks, and the harmless end-to-end test. The pack lives at `.agents/pstack/automations/benny/`. Read its three instruction files by committed path. Project-local Codex discovery provides the shared pstack skills. Use the following transport steps in place of the Cursor Automations editor.

1. Fill user-owned Slack, tracker, repo, routing, control, feature map, and budget settings outside the pack. The receiver requires the Slack app ID, team ID, channel ID, and trusted triage user ID. The worker requires both configuration and feature map to exist at its pinned Git revision.
2. Confirm fresh Codex discovery of the shared `how`, `why`, `tdd`, `unslop`, and principle skills. Configure thread read and thread reply, attachment download, tracker search and write, and draft PR access for the coordinator. Use an actual target control adapter that fulfills the [control contract](../automations/benny/skills/reproduce-and-fix-issues/references/control-adapter.md), including real UI, screenshot, video, state cross-check, and cleanup.
3. Add the following `slack` object to the bridge's secret-free config. Fill real values and match the configured triage posting identity. Set `BENNY_SLACK_SIGNING_SECRET` from the Slack app secret manager in the receiver environment only.

```json
{
  "app_id": "<Slack app ID>",
  "team_id": "<Slack team ID>",
  "source_channel_id": "<source Slack channel ID>",
  "triage_identity_user_id": "<triage Slack user ID>",
  "configuration_path": "docs/automation/benny/configuration.yaml",
  "feature_map_path": "docs/automation/benny/feature-map.md"
}
```

4. When the user asks to activate the automations, configure the signed Slack Events endpoint and start the receiver, then a worker for `benny-triage` and a worker for `benny-reproduce`. Choose `--timeout` in seconds to cover each configured run budget. The receiver validates the [Slack signature](https://docs.slack.dev/authentication/verifying-requests-from-slack/) and filters event types, channel, app, team, and root coordinates. Each matching root queues one run per role, even when Slack retries. Repro reads and waits for the trusted marker in that exact thread.
5. In a disposable channel, test the full thread-safety checklist. Verify one triage verdict, no root post, trusted-author gating, same root for both workers, no delegated Slack writes, and no side effects for missing coordinates or failed setup. Verify the repro path twice with the selected control adapter before authorizing it to prepare draft PRs. Preserve the upstream read-only verification path for an existing PR.

Worker execution needs working Codex authentication and the chosen repo, Slack, and tracker integrations. Use `--codex`, `--model`, and `--effort` to match the executable and its account. Keep connector credentials limited to the configured resources and authorized actions. Where the worker cannot restrict delegated Slack capabilities, run the two coordinators without subagents. Credentials for the webhook receiver are removed from the worker environment. The operational files enforce the thread-only and draft-only rules at the coordinator. Validate these rules using the actual connector before enabling normal traffic. Run a connector authorization check for the detached-worktree environment as well as the interactive session.

## Local verification

```bash
python3 -B -m unittest discover -s .agents/pstack/host -v
```

The tests cover valid and invalid signatures, a signed Slack challenge, source coordinate checks, delivery deduplication, bearer keys, secret removal, worktree isolation, timeout, and SIGTERM with a test Codex executable. Live Slack delivery and authenticated Codex remote actions require the configured integrations and a harmless end-to-end run.
