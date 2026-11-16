#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "앱 바인드 마운트 경로를 준비하려면 root 권한으로 실행하세요." >&2
  exit 1
fi

install -d -o root -g root -m 0750 /var/lib/jbnu-sugang-helper

install -d -o root -g root -m 0750 \
  /var/lib/jbnu-sugang-helper/loki \
  /var/lib/jbnu-sugang-helper/alloy \
  /var/lib/jbnu-sugang-helper/prometheus

install -d -o 10001 -g 10001 -m 0750 /var/lib/jbnu-sugang-helper/loki
install -d -o 472 -g 472 -m 0750 /var/lib/jbnu-sugang-helper/grafana

install -d -o 65534 -g 65534 -m 0750 /var/lib/jbnu-sugang-helper/prometheus
