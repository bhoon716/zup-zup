#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this script as root to prepare app bind mounts" >&2
  exit 1
fi

db_data_dir="${DB_DATA_DIR:-/var/lib/jbnu-sugang-helper/mysql}"
install -d -o root -g root -m 0750 /var/lib/jbnu-sugang-helper
if [ ! -d "${db_data_dir}" ] || ! mountpoint -q "${db_data_dir}"; then
  echo "DB_DATA_DIR must be an actual mounted OCI block-volume filesystem: ${db_data_dir}" >&2
  exit 1
fi
db_source="$(findmnt -n -T "${db_data_dir}" -o SOURCE 2>/dev/null || true)"
if [[ ! "${db_source}" =~ ^(/dev/|UUID=|LABEL=) ]]; then
  echo "DB_DATA_DIR mount source is not a block-volume device: ${db_source:-unknown}" >&2
  exit 1
fi
install -d -o root -g root -m 0750 "${db_data_dir}"

# The app stores uploads in a named Docker volume. No Redis data directory is
# created because Redis is intentionally ephemeral in the minimal runtime.
install -d -o root -g root -m 0750 \
  /var/log/jbnu-sugang-helper \
  /var/lib/jbnu-sugang-helper/loki \
  /var/lib/jbnu-sugang-helper/alloy \
  /var/lib/jbnu-sugang-helper/prometheus

# Loki runs as UID/GID 10001 and Grafana as UID/GID 472 in the official images.
# Alloy runs as root because it tails the Docker socket and host logs.
install -d -o 10001 -g 10001 -m 0750 /var/lib/jbnu-sugang-helper/loki
install -d -o 472 -g 472 -m 0750 /var/lib/jbnu-sugang-helper/grafana
# Prometheus runs as the official image's `nobody` user.
install -d -o 65534 -g 65534 -m 0750 /var/lib/jbnu-sugang-helper/prometheus
