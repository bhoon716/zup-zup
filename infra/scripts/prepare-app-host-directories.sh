#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "run this script as root to prepare app bind mounts" >&2
  exit 1
fi

install -d -o 10001 -g 10001 -m 0750 \
  /var/log/jbnu-sugang-helper/server \
  /var/lib/jbnu-sugang-helper/uploads

install -d -o 999 -g 1000 -m 0700 \
  /var/lib/jbnu-sugang-helper/redis
