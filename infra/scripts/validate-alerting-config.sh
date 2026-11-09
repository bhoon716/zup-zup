#!/usr/bin/env bash
# Validate offline with an already available, operator-verified official image.
set -euo pipefail

log_validation_failure() {
  printf '{"event":"alerting_configuration","schemaVersion":1,"status":"invalid","reason":"%s","message":"%s"}\n' "$1" "$2" >&2
}

log_binary_validation_success() {
  printf '%s\n' '{"event":"alerting_configuration","schemaVersion":1,"status":"valid","reason":"binary_config_and_route_validated","message":"설정과 합성 경보 라우팅 검증 통과. 외부 전송은 하지 않았습니다."}'
}

script_dir="$(cd "$(dirname "$0")" && pwd)"
infra_dir="$(cd "${script_dir}/.." && pwd)"
if [ "${1:-}" = --static-only ] && [ "$#" -eq 1 ]; then
  exec python3 "${script_dir}/validate-alerting-settings.py"
fi
if [ "$#" -ne 0 ]; then
  echo '사용법: validate-alerting-config.sh [--static-only]' >&2
  exit 2
fi
image="${ALERTMANAGER_IMAGE:-}"
if ! [[ "${image}" =~ ^prom/alertmanager@sha256:[a-f0-9]{64}$ ]]; then
  log_validation_failure image_digest_invalid '승인·검증된 prom/alertmanager sha256 digest가 필요합니다.'
  exit 2
fi

temporary_dir="$(mktemp -d)"
cleanup() {
  rm -rf -- "${temporary_dir}"
}
trap cleanup EXIT

# This reserved .invalid fixture is never an actual Slack endpoint or credential.
printf '%s\n' 'https://example.invalid/alert-validation-fixture' >"${temporary_dir}/webhook"
chmod 0644 "${temporary_dir}/webhook"
config="${infra_dir}/alertmanager/alertmanager.yml"
if [ ! -f "${config}" ]; then
  log_validation_failure config_missing 'Alertmanager 설정 파일이 없습니다.'
  exit 2
fi

validation=(docker run --rm --pull never --network none --read-only --cap-drop ALL
  --security-opt no-new-privileges:true --entrypoint /bin/amtool
  -v "${config}:/etc/alertmanager/alertmanager.yml:ro"
  -v "${temporary_dir}/webhook:/run/secrets/alertmanager_slack_webhook:ro"
  "${image}")
"${validation[@]}" check-config /etc/alertmanager/alertmanager.yml
"${validation[@]}" config routes test --config.file=/etc/alertmanager/alertmanager.yml \
  --verify.receivers=operations-slack alertname=AlertDeliverySmokeTest severity=info service=sugang-helper test_id=fixture
log_binary_validation_success
