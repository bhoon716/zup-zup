# pstack on Codex

Start with `$pstack:poteto-mode <goal>` in Codex Desktop, or name a skill directly. Use `$pstack:poteto-help` for workflow selection and `$pstack:setup-pstack` for the role model sheet. Older CLI builds list short names such as `$poteto-mode`. The project root [AGENTS.md](../../AGENTS.md) also routes nontrivial work through poteto-mode. Keep the originating user request and the playbook's finish conditions in view.

## Source and loading

The [repository lock](upstream.lock.json) pins Lauren Tan's pstack 0.15.36 at [`63912a91`](https://github.com/cursor/plugins/commit/63912a91d52c43946f974f2aaf661334a6116a82). Install it once per checkout with Git, patch, and Python 3.12 or later:

```sh
python3 .agents/pstack/install.py
```

The installer verifies the full upstream tree, records file hashes in local `upstream.json`, and links 61 skills for Codex. The 58 core skills, 23 playbooks, 24 principles, both personas, and `deslop`, `control-cli`, and `control-ui` use the same pinned commit. The downloaded sources, test fixtures, and generated links are local to the checkout. A second install verifies the existing files. Open the committed [Benny operator entry point](automations/benny/FOR_AGENTS.md) directly as its setup requires.

[Project configuration](../../.codex/config.toml) registers the two named agents. [SessionStart](../../.codex/hooks.json) restores the routing cue on startup, resume, clear, compaction, and fork. Codex runs a project hook after its definition is trusted in `/hooks`. AGENTS.md supplies the project route in fresh sessions independently of hook trust. [Codex skills](https://learn.chatgpt.com/docs/build-skills) · [Codex hooks](https://learn.chatgpt.com/docs/hooks) · [Codex subagents](https://learn.chatgpt.com/docs/agent-configuration/subagents)

Treat the original skill and playbook steps as the workflow. Apply the host mappings below wherever those files name Cursor primitives or paths. User instructions and the project's coding conventions take precedence.

## Tool mapping

| Upstream instruction | Execute on Codex |
| --- | --- |
| `/skill-name`, built-in `Skill`, custom mode | Load the matching local `SKILL.md` in full. Use the skill mention shown by the current Codex catalog. The current chat remains the coordinator. |
| Todo list, TodoWrite | Use the host plan tool if exposed, with the exact playbook steps first. Track the same ordered list in the run checkpoint when the plan tool is unavailable or the run spans sessions. Keep skipped steps and their reasons. |
| Task spawn, monitor, resume | Use the host's subagent dispatch and wait tools. Start each review with fresh history and a self-contained brief. Wait for the accepted handles, read the reports, close slots if supported, and start the next wave. Apply the playbook's restrictions on reusing agents. |
| `poteto-agent`, `comment-sicko` | Pass the named agent type if the session exposes it. Otherwise provide the original persona and required reading as the first lines of the delegate brief. Always pass the absolute path to `poteto-mode/SKILL.md` to the poteto-agent. |
| Agent mode, readonly mode, cloud VM | Give each concurrent writer or mutable live verifier a separate worktree and isolated runtime state. Give reviewers a fixed revision and a read-only brief, and use a read-only sandbox or isolated checkout where the host allows it. Share only the inputs named in the playbook. Keep independent checks independent. |
| `AskQuestion` | Use the host's structured user input when available. Otherwise ask in plain text. Apply the same policy for observable facts and product choices. |
| `/create-skill`, installed skills, instructions file | Use Codex's skill authoring workflow. Project skills go in `.agents/skills/<name>/`; local user skills go in `$HOME/.agents/skills/`. Project instructions are `AGENTS.md`. For `reflect`, edit a project or user overlay when that is the chosen scope. |
| `/deslop` | Load the [pinned deslop skill](cursor-team-kit/skills/deslop/SKILL.md). Apply its diff-focused cleanup with the project's conventions. |
| `control-ui`, `control-cli`, image generation | Use the corresponding pinned control skill for workflow and the available Codex browser, terminal or image tool for execution. Use the browser the user named. Record the same actual interaction and proof. See the [host adapters](host/README.md). |
| Cursor cloud URLs, `/drive`, evidence attachments | Use the available Codex thread and workspace artifact handles. Carry forward a real branch, revision, screenshot or transcript reference. Retain a concrete local evidence path when the destination cannot embed the evidence. |
| Cursor / GitHub / Origin PR tooling | Resolve the forge with the original playbook. Use the included `scripts/watch-pr/watch-pr` for GitHub status and the playbook's `gh` or Origin commands for mutations. Use existing review comments and checks as the source of truth. |
| Cursor automations and webhook wake | Use the [host adapters](host/README.md) with the original templates. Each external service requires its own configured endpoint and credentials. |

When the installed Codex build cannot dispatch a named or model-selected role natively, use the persona briefing with a supported model override. For a CLI-backed independent arm, run `codex exec -C <isolated-worktree> -m <available-model> -c model_reasoning_effort=<effort>` using the same brief and appropriate sandbox. Keep the requested candidate count across waves. An unavailable required review or missing runtime evidence leaves its gate open.

For `poteto-help`, use the installed skill list and this document to answer questions about this project's setup. For `automate-me`, find the user's mode under the project or user skill root, gather only the project's scoped transcripts, and place proposed modes or automations in those same project or user locations. For `no-comments`, keep the original comment-sicko persona and report format. When `teach` uses images, attach output from the active image generation tool.

## Models and effort

Read the [project role sheet](../../.codex/pstack-models.md) before dispatch. It maps all upstream role rows to Codex models. A row is a slug followed by `@effort`. Comma-separated values create the upstream panel arms. `inherit-parent` and `auto` preserve the parent selection. Validate model availability against the current host's model catalog or actual dispatch tool. In sessions with `fork_turns`, use `none` for independent reviewers and when a model override requires it.

The initial sheet uses `gpt-6.1-sol` for code and `gpt-6-astra` for judgment, with `xhigh` as the starting effort. These are listed by the installed Codex Desktop runtime. A separate CLI installation can have a different catalog and account entitlements. Validate role choices on the runtime that will run them.

Apply the upstream per-task reasoning adjustment, respecting the valid efforts for that model. Codex expresses effort separately from the model ID. Use standard service by default and fast service for the original urgent cases when the host exposes that control. Try a supported starting effort if an adjusted choice is rejected. If the available families differ from the sheet, choose the closest supported role and report the substitution. Same-family runs can supply independent exploration but cannot establish cross-family agreement.

For `$setup-pstack`, use the original sequence for detecting models, selecting a budget, presenting all role rows, and validating choices. Write the whole result to `.codex/pstack-models.md`. Keep the role labels and panel ordering and replace Cursor's suffix-based effort with the separate `@effort` field. The four original budget choices map to max (when supported), xhigh, high and medium. Retain role-specific models on a budget-only change.

## Loops, checkpoints and history

- For Cursor `/loop` in an active run, start the original event watcher or watcher delegate, wait on its accepted process or tool handle, and process new results until the playbook's checkable predicate is met. Use a bounded wait between observations so the user can steer. For GitHub Babysit, keep the same frontier and re-arm the watcher after each push or acted-on verdict. Use timed checks as the heartbeat only where the playbook calls for one.
- Keep the run's plan, last check and evidence in its worktree. For handoff, persist the next step, predicate, exact revision, and any running process or subagent ID. Use the existing Codex goal or thread to continue a task. Reserve a standing automation for a recurring job requested by the user.
- Use the [Codex transcript resolver](transcripts/README.md) wherever the upstream workflow references `agent-transcripts`. Reflect and the decision audit use the current thread. Recall and pickup search the current project and registered worktrees. Eval reads the actual candidate sessions using their IDs. Archived rollouts require an explicit archive search. For unavailable current or candidate transcripts, retain the skill's missing-evidence branch and label the limit.
- Worktree cleanup uses the original branch, PR, status and recency classifications with Codex session activity. When Codex activity cannot be read, keep the affected candidate for manual review.

### Multi-phase program

In the original program template, replace repository-relative `pstack/skills/` with `.agents/pstack/skills/`. Run the plan checker from the project root:

```sh
node .agents/pstack/skills/poteto-mode/scripts/check-plan.mjs <plan.md>
```

Keep the plan's ten live lanes and its `/loop 1h` checklist marker. Execute that marker as the in-session audit cadence above. In the program template's trunk-read step, run `git show origin/main:.agents/pstack/upstream.lock.json` at startup and at each tick. Compare it with the local lock and run `python3 .agents/pstack/verify.py` before reading the installed playbook and named skills. This checks the revision selected on trunk and the bytes read from it. When the locks differ, use a checkout of the matching trunk revision and install there before proceeding. A branch from before installation can use the coordinator's absolute skill paths for planning and delegation.

## Verification and archive

Run `python3 .agents/pstack/verify.py` for the pinned file and catalog checks and `python3 .agents/pstack/verify_runtime.py` for a fresh Codex discovery probe. The worktree audit has a [Codex-specific patch](patches/worktree-audit.patch). The original plan checker and GitHub watcher retain their tests in [poteto-mode scripts](skills/poteto-mode/scripts). The old project harness is stored locally under `.agents/archeive/`. Add `--archive` to `verify.py` to compare it with the migration snapshot. The project conventions are independently published at [`../conventions`](../conventions).

On this Mac, use `--codex "/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex"` with the runtime probe to select the Desktop executable explicitly. The standalone Homebrew CLI's catalog listed models that its account rejected at execution.

The comparison ports informed the runtime mappings. [Denyer's Codex adapter](https://github.com/michael-denyer/pstack-claude/blob/bc09365fe61c725c486ba95157fc4048bf8a0b99/plugins/pstack/skills/poteto-mode/references/codex-tools.md) · [mdsmithaustin's Codex adapter](https://github.com/mdsmithaustin/pstack/blob/5fc2d8b5c234d753387d6090f0575176767daf06/skills/pstack-harness/references/codex.md)
