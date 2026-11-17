#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")"
name=sugang-helper-npm
project="sugang-helper-edge-$(date -u +%Y%m%d%H%M%S)"
backup="${name}-rollback-$(date -u +%Y%m%dT%H%M%SZ)"
state=/home/ubuntu/jbnu-sugang-helper/infra/npm
snapshot="$state-backup-$(date -u +%Y%m%dT%H%M%SZ).tgz"
docker compose -p "$project" -f compose.yml config -q
docker inspect "$name" >/dev/null
test -d "$state/data" && test -d "$state/letsencrypt"
docker image inspect jc21/nginx-proxy-manager@sha256:2aa69b382a384b676c0d4f1d6f2eac40ecd478fcf7af1cfb3f9f1d3cd0c81e12 >/dev/null
sudo install -m 600 /dev/null "$snapshot"
# Stop before snapshot so the SQLite database and its journal are consistent.
trap 'docker start "$name" >/dev/null; exit 1' ERR HUP INT TERM
docker stop "$name"
rollback() {
  trap - ERR HUP INT TERM
  docker rm -f "$name" >/dev/null 2>&1 || true
  docker rename "$backup" "$name"
  docker start "$name"
  echo 'Deployment failed; previous container restored.' >&2
  exit 1
}
# A failed snapshot must not leave the public proxy stopped.
if ! sudo tar czf "$snapshot" -C "$state" data letsencrypt; then
  docker start "$name"
  exit 1
fi
sudo chmod 600 "$snapshot"
docker rename "$name" "$backup"
trap rollback ERR HUP INT TERM
docker compose -p "$project" -f compose.yml up -d --pull never
for attempt in $(seq 1 60); do
  if curl --max-time 3 --fail --silent http://127.0.0.1:81/ >/dev/null; then break; fi
  sleep 2
done
curl --max-time 3 --fail --silent http://127.0.0.1:81/ >/dev/null
test "$(docker inspect "$name" --format '{{(index (index .HostConfig.PortBindings "81/tcp") 0).HostIp}}')" = 127.0.0.1
docker exec "$name" nginx -t
trap - ERR HUP INT TERM
echo "NPM admin is localhost-only. Preserved rollback container: $backup"
echo "Private data snapshot: $snapshot"
