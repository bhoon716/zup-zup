#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
systemd_dir="${repo_root}/infra/systemd"

for required_file in \
  jbnu-sugang-helper-dr-backup.service \
  jbnu-sugang-helper-dr-backup.timer \
  jbnu-sugang-helper-redis-backup.service \
  jbnu-sugang-helper-redis-backup.timer \
  jbnu-sugang-helper-backup-freshness.service \
  jbnu-sugang-helper-backup-freshness.timer \
  jbnu-sugang-helper-backup-failure@.service; do
  if [ ! -f "${systemd_dir}/${required_file}" ]; then
    echo "missing backup automation unit: ${required_file}" >&2
    exit 1
  fi
done

grep -F -- 'OnCalendar=*-*-* 03:17:00' "${systemd_dir}/jbnu-sugang-helper-dr-backup.timer" >/dev/null
grep -F -- 'OnCalendar=*-*-* 03:47:00' "${systemd_dir}/jbnu-sugang-helper-redis-backup.timer" >/dev/null
grep -F -- 'Persistent=true' "${systemd_dir}/jbnu-sugang-helper-dr-backup.timer" >/dev/null
grep -F -- 'OnFailure=jbnu-sugang-helper-backup-failure@%n.service' "${systemd_dir}/jbnu-sugang-helper-dr-backup.service" >/dev/null
grep -F -- 'OnFailure=jbnu-sugang-helper-backup-failure@%n.service' "${systemd_dir}/jbnu-sugang-helper-redis-backup.service" >/dev/null
grep -F -- 'verify-backup-freshness.sh' "${systemd_dir}/jbnu-sugang-helper-backup-freshness.service" >/dev/null

set +e
output="$(ALLOW_NON_ROOT=1 bash "${repo_root}/infra/scripts/backup-log-state.sh" 2>&1)"
rc=$?
set -e
if [ "${rc}" -ne 2 ] || ! grep -F -- 'deprecated' <<<"${output}" >/dev/null; then
  echo "backup-log-state.sh must fail closed as deprecated" >&2
  exit 1
fi

echo "backup policy verification passed"
