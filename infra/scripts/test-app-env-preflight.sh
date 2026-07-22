#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"
temporary_dir="$(mktemp -d)"
fixture="${temporary_dir}/app.env"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

write_fixture() {
  printf '%s\n' "$@" >"${fixture}"
}

assert_rejected() {
  local case_name="$1"
  shift
  write_fixture "$@"
  if "${deploy_script}" --validate-app-env "${fixture}" >/dev/null 2>&1; then
    echo "app env preflight must reject ${case_name}" >&2
    exit 1
  fi
}

write_fixture \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google"
"${deploy_script}" --validate-app-env "${fixture}" >/dev/null

assert_rejected "a missing redirect URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret"
assert_rejected "an empty redirect URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI="
assert_rejected "a quoted empty redirect URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  'GOOGLE_REDIRECT_URI=""'
assert_rejected "the local callback URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=http://localhost:3000/api/login/oauth2/code/google"
assert_rejected "a malformed redirect URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=not-a-uri"
assert_rejected "duplicate redirect URI assignments" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google" \
  "GOOGLE_REDIRECT_URI="

echo "application env preflight contract passed"
