#!/usr/bin/env bash
set -euo pipefail

cat >&2 <<'EOF'
backup-log-state.sh is deprecated and intentionally disabled.
Prometheus, Loki, Alertmanager, and Promtail state are disposable observability data.
Use backup-dr-state.sh for encrypted durable-state backups and verify-backup-freshness.sh for freshness checks.
EOF
exit 2
