#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
deploy_script="${repo_root}/infra/scripts/deploy-release.sh"
example_env="${repo_root}/apps/server/.env.example"
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
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google" \
  "DISCORD_CLIENT_ID=test-discord-client" \
  "DISCORD_CLIENT_SECRET=test-discord-secret" \
  "DISCORD_REDIRECT_URI=https://www.zup-zup.com/api/v1/users/discord/callback"
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

assert_rejected "a missing Discord redirect URI" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google" \
  "DISCORD_CLIENT_ID=test-discord-client" \
  "DISCORD_CLIENT_SECRET=test-discord-secret"
assert_rejected "the stale Discord auth callback path" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google" \
  "DISCORD_CLIENT_ID=test-discord-client" \
  "DISCORD_CLIENT_SECRET=test-discord-secret" \
  "DISCORD_REDIRECT_URI=https://www.zup-zup.com/api/v1/auth/discord/callback"
assert_rejected "a Discord redirect URI without the canonical www host" \
  "GOOGLE_CLIENT_ID=test-client" \
  "GOOGLE_CLIENT_SECRET=test-secret" \
  "GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google" \
  "DISCORD_CLIENT_ID=test-discord-client" \
  "DISCORD_CLIENT_SECRET=test-discord-secret" \
  "DISCORD_REDIRECT_URI=https://zup-zup.com/api/v1/users/discord/callback"

if [ "$(awk -F= '$1 == "DISCORD_REDIRECT_URI" { print $2 }' "${example_env}")" \
  != "http://localhost:3000/api/v1/users/discord/callback" ]; then
  echo "application env example must use the local Discord users callback" >&2
  exit 1
fi

echo "application env preflight contract passed"
