#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"
rollback_script="${repo_root}/infra/scripts/rollback-release.sh"
installer="${repo_root}/infra/scripts/install-oci-wrappers.sh"

for required in \
  'GHCR_USERNAME_FILE' \
  'GHCR_TOKEN_FILE' \
  'docker login ghcr.io' \
  '--password-stdin' \
  'GHCR authentication failed'; do
  if ! grep -F -- "${required}" "${deploy_script}" >/dev/null; then
    echo "deploy GHCR auth contract is missing: ${required}" >&2
    exit 1
  fi
  if ! grep -F -- "${required}" "${rollback_script}" >/dev/null; then
    echo "rollback GHCR auth contract is missing: ${required}" >&2
    exit 1
  fi
done

for required in GHCR_READ_USERNAME GHCR_READ_TOKEN_SOURCE ghcr-read-token; do
  if ! grep -F -- "${required}" "${installer}" >/dev/null; then
    echo "OCI installer must provision ${required}" >&2
    exit 1
  fi
done

echo "GHCR root-auth contract passed"
