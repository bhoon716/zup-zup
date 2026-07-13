#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this script as root to prepare app bind mounts" >&2
  exit 1
fi

install -d -o root -g root -m 0755 \
  /var/log/jbnu-sugang-helper \
  /var/lib/jbnu-sugang-helper

install -d -o 10001 -g 10001 -m 0750 \
  /var/log/jbnu-sugang-helper/server \
  /var/lib/jbnu-sugang-helper/uploads

install -d -o 472 -g 472 -m 0750 \
  /var/lib/jbnu-sugang-helper/grafana

install -d -o 999 -g 1000 -m 0700 \
  /var/lib/jbnu-sugang-helper/redis

install -d -o 65534 -g 65534 -m 0750 \
  /var/lib/jbnu-sugang-helper/prometheus \
  /var/lib/jbnu-sugang-helper/alertmanager

infra_dir="$(cd "$(dirname "$0")/.." && pwd)"
install -d -o root -g root -m 0700 \
  "${infra_dir}/nginx-proxy-manager/data" \
  "${infra_dir}/nginx-proxy-manager/letsencrypt"
