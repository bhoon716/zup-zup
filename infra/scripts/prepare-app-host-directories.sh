#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this script as root to prepare app bind mounts" >&2
  exit 1
fi

install -d -o root -g root -m 0750 /var/lib/jbnu-sugang-helper

# The app stores uploads and MySQL data in Docker-managed named volumes. The
# observability profile uses only these host-backed directories. Redis is
# intentionally ephemeral and Docker JSON logs are collected through Alloy.
install -d -o root -g root -m 0750 \
  /var/lib/jbnu-sugang-helper/loki \
  /var/lib/jbnu-sugang-helper/alloy \
  /var/lib/jbnu-sugang-helper/prometheus

# Loki runs as UID/GID 10001 and Grafana as UID/GID 472 in the official images.
# Alloy runs as root because it tails the Docker socket and host logs.
install -d -o 10001 -g 10001 -m 0750 /var/lib/jbnu-sugang-helper/loki
install -d -o 472 -g 472 -m 0750 /var/lib/jbnu-sugang-helper/grafana
# Prometheus runs as the official image's `nobody` user.
install -d -o 65534 -g 65534 -m 0750 /var/lib/jbnu-sugang-helper/prometheus
