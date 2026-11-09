#!/usr/bin/env bash
# CI-only integration contract. Uses synthetic numbers, no host or Docker socket mount.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
tmp="$(mktemp -d)"
name="sugang-host-metrics-fixture-$$"
cleanup() {
  docker rm -f "$name" >/dev/null 2>&1 || true
  rm -rf "$tmp"
}
trap cleanup EXIT
alloy_image="$(awk '/^  alloy:$/ {found=1; next} found && /^    image:/ {print $2; exit}' "$root/infra/docker-compose.yml")"
if [[ "$alloy_image" != grafana/alloy@sha256:* ]]; then
  echo 'Alloy fixture must use the pinned release image' >&2
  exit 1
fi
mkdir "$tmp/metrics"
cat > "$tmp/config.alloy" <<'EOF'
prometheus.exporter.unix "host_jobs" {
  set_collectors = ["textfile"]
  textfile {
    directory = "/fixtures"
  }
}
EOF
cat > "$tmp/metrics/storage.prom" <<'EOF'
sugang_host_job_success{task="storage"} 1
sugang_host_job_last_success_timestamp_seconds{task="storage"} 123
sugang_host_storage_healthy{role="release"} 1
sugang_host_storage_healthy{role="docker_data"} 1
EOF
# host port is ephemeral and localhost-only; no production services are touched.
docker run --detach --name "$name" \
  --read-only --cap-drop ALL --security-opt no-new-privileges \
  --tmpfs /tmp --user "0:0" \
  -p 127.0.0.1::12345 \
  -v "$tmp/config.alloy:/etc/alloy/config.alloy:ro" \
  -v "$tmp/metrics:/fixtures:ro" \
  "$alloy_image" run --server.http.listen-addr=0.0.0.0:12345 \
  --storage.path=/tmp/alloy /etc/alloy/config.alloy >/dev/null
port="$(docker port "$name" 12345/tcp | awk -F: '{print $NF}')"
python3 - "$port" <<'PY'
import sys
import time
import urllib.error
import urllib.request
url = 'http://127.0.0.1:' + sys.argv[1] + '/api/v0/component/prometheus.exporter.unix.host_jobs/metrics'
text = None
for attempt in range(40):
    try:
        with urllib.request.urlopen(url, timeout=2) as response:
            text = response.read(1024 * 1024).decode()
        break
    except (OSError, urllib.error.URLError):
        time.sleep(0.25)
if text is None:
    raise SystemExit('Alloy component endpoint did not become available')
for expected in ('sugang_host_job_success{task="storage"} 1',
                 'sugang_host_storage_healthy{role="release"} 1',
                 'sugang_host_storage_healthy{role="docker_data"} 1',
                 'node_textfile_scrape_error 0'):
    if expected not in text:
        raise SystemExit('Host textfile telemetry contract is missing')
if 'node_cpu_seconds_total' in text or 'node_filesystem_size_bytes' in text:
    raise SystemExit('Host exporter must enable only the textfile collector')
print('Host textfile component endpoint contract passed')
PY
