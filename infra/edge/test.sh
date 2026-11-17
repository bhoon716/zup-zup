#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
bash -n infra/edge/deploy.sh
config=$(mktemp)
trap 'rm -f "$config"' EXIT
docker compose -f infra/edge/compose.yml config --format json > "$config"
python3 - "$config" <<'PY'
import json, sys
from pathlib import Path
from fnmatch import fnmatchcase
c = json.loads(Path(sys.argv[1]).read_text())
npm = c['services']['npm']
assert {(p.get('host_ip', ''), str(p['published']), p['target']) for p in npm['ports']} == {('', '80', 80), ('127.0.0.1', '81', 81), ('', '443', 443)}
assert npm['image'].endswith('@sha256:2aa69b382a384b676c0d4f1d6f2eac40ecd478fcf7af1cfb3f9f1d3cd0c81e12')
assert {v['target'] for v in npm['volumes']} == {'/data', '/etc/letsencrypt'}
assert {n['name'] for n in c['networks'].values()} == {'sugang-helper-runtime', 'sugang-helper-network-server'}
cd = Path('.github/workflows/cd.yml').read_text()
paths = cd.split('    paths:\n', 1)[1].split('  workflow_dispatch:', 1)[0]
patterns = [line.strip().removeprefix('- ').strip('"') for line in paths.splitlines() if line.strip().startswith('- ')]
def deploys(path):
    result = False
    for pattern in patterns:
        if fnmatchcase(path, pattern.lstrip('!')):
            result = not pattern.startswith('!')
    return result
for path in ('infra/edge/compose.yml', 'infra/edge/deploy.sh', 'infra/edge/test.sh', 'infra/edge/README.md', '.github/workflows/edge-cd.yml', '.github/workflows/cd.yml'):
    assert not deploys(path), f'edge-only change triggers app deployment: {path}'
for path in ('apps/server/Dockerfile', 'infra/docker-compose.yml', 'infra/scripts/deploy-release.sh'):
    assert deploys(path), f'app deployment trigger lost: {path}'
print('edge binding, image, networks, and independent CD path contracts passed')
PY
