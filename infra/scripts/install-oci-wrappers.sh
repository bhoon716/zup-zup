#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this installer as root" >&2
  exit 1
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
release_root="/opt/jbnu-sugang-helper"
manifest_public_key_source="${DEPLOY_MANIFEST_PUBLIC_KEY_SOURCE:-}"
ghcr_read_username="${GHCR_READ_USERNAME:-}"
ghcr_read_token_source="${GHCR_READ_TOKEN_SOURCE:-}"
if [ -z "${manifest_public_key_source}" ] || [ ! -f "${manifest_public_key_source}" ]; then
  echo "DEPLOY_MANIFEST_PUBLIC_KEY_SOURCE must point to the OCI manifest public key" >&2
  exit 1
fi
if [[ ! "${ghcr_read_username}" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "GHCR_READ_USERNAME must be a GitHub username" >&2
  exit 1
fi
if [ -z "${ghcr_read_token_source}" ] || [ ! -f "${ghcr_read_token_source}" ] \
  || [ -L "${ghcr_read_token_source}" ]; then
  echo "GHCR_READ_TOKEN_SOURCE must point to a regular token file" >&2
  exit 1
fi
install -d -o root -g root -m 0755 /usr/local/sbin /usr/local/libexec/jbnu-sugang-helper
install -d -o root -g root -m 0700 "${release_root}/secrets"
install -o root -g root -m 0600 "${manifest_public_key_source}" \
  "${release_root}/secrets/deploy-manifest-public.pem"
printf '%s\n' "${ghcr_read_username}" >"${release_root}/secrets/ghcr-read-username.tmp"
install -o root -g root -m 0600 "${release_root}/secrets/ghcr-read-username.tmp" \
  "${release_root}/secrets/ghcr-read-username"
rm -f "${release_root}/secrets/ghcr-read-username.tmp"
install -o root -g root -m 0600 "${ghcr_read_token_source}" \
  "${release_root}/secrets/ghcr-read-token"
install -o root -g root -m 0755 "${repo_root}/infra/scripts/oci/jbnu-deploy" /usr/local/sbin/jbnu-deploy
install -o root -g root -m 0755 "${repo_root}/infra/scripts/oci/jbnu-rollback" /usr/local/sbin/jbnu-rollback
install -o root -g root -m 0755 "${repo_root}/infra/scripts/oci/jbnu-nginx-config-apply" /usr/local/sbin/jbnu-nginx-config-apply
install -o root -g root -m 0755 "${repo_root}/infra/scripts/deploy-release.sh" /usr/local/libexec/jbnu-sugang-helper/deploy-release
install -o root -g root -m 0755 "${repo_root}/infra/scripts/rollback-release.sh" /usr/local/libexec/jbnu-sugang-helper/rollback-release
echo "installed allowlisted OCI wrappers"
