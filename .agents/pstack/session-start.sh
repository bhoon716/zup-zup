#!/bin/sh
set -eu

[ -f "$(dirname "$0")/skills/poteto-mode/SKILL.md" ] || exit 0

cat <<'CONTEXT'
pstack is available in this project. Follow AGENTS.md and .agents/pstack/CODEX.md.
Use the local poteto-mode skill and matching playbook for a nontrivial change, design
choice, undiagnosed bug, performance issue, or PR coordination. A named skill
can be entered directly. Read .codex/pstack-models.md before choosing role
models. If a pstack run is already in progress, restore its worklist and
verification evidence and continue from the first unfinished step.
CONTEXT
