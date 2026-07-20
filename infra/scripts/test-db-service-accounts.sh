#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="${repo_root}/infra/mysql/init/01-provision-service-accounts.sh"

if [ ! -x "${script}" ]; then
  echo "DB service-account provisioner is missing or not executable" >&2
  exit 1
fi
bash -n "${script}"

python3 - "${script}" <<'PY'
import sys
from pathlib import Path

content = Path(sys.argv[1]).read_text(encoding="utf-8")
for required in ("DB_RUNTIME_USER", "DB_RUNTIME_PASSWORD", "DB_MIGRATOR_USER", "DB_MIGRATOR_PASSWORD"):
    if required not in content:
        raise SystemExit(f"missing required runtime account variable: {required}")
if "DB_BACKUP_" in content:
    raise SystemExit("DB backup account must remain outside the minimal runtime contract")
if "GRANT SELECT, INSERT, UPDATE, DELETE" not in content:
    raise SystemExit("runtime account must receive DML privileges")
if "CREATE, ALTER, DROP" not in content:
    raise SystemExit("migrator account must receive migration DDL privileges")
if "service accounts must not use the root username" not in content:
    raise SystemExit("root account must not be used as a service account")
print("minimal DB service-account contract passed")
PY
