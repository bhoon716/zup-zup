#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
python3 - "${repo_root}/infra/scripts/deploy-release.sh" <<'PY'
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

source = Path(sys.argv[1]).read_text()
fixed_root = 'readonly RELEASE_ROOT="/home/ubuntu/jbnu-sugang-helper"'
assert source.count(fixed_root) == 1, "production release root must stay fixed"

docker_stub = '''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
args = sys.argv[1:]
scenario = os.environ['SCENARIO']
event = 'other'
code, output = 0, ''
if args[:2] == ['network', 'inspect']:
    output = 'sugang-helper-npm'
elif args and args[0] == 'inspect' and '--type' in args:
    event = 'inspect-app'
    if scenario == 'inspect-fails': code = 19
    elif scenario == 'still-running': output = 'true'
    elif scenario == 'invalid-state': output = 'unknown'
    elif scenario == 'second-running' and args[-1].endswith('2'): output = 'true'
    else: output = 'false'
elif args and args[0] == 'compose':
    if 'stop' in args:
        event = 'stop'
        if scenario == 'stop-fails': code = 73
    elif 'ps' in args:
        event = 'ps-app'
        if scenario == 'ps-fails': code = 17
        elif scenario == 'invalid-id': output = 'not-a-container-id'
        elif scenario != 'no-existing-app':
            output = 'a' * 63 + '1'
            if scenario == 'second-running': output += '\\n' + 'b' * 63 + '2'
    elif 'run' in args and 'migrate' in args:
        event = 'migrate'
        if scenario == 'migration-fails': code = 23
    elif 'up' in args and 'app' in args:
        event = 'app-start'
        if scenario == 'startup-fails': code = 29
with Path(os.environ['COMMAND_TRACE']).open('a') as stream:
    stream.write(json.dumps({'event': event, 'args': args}) + '\\n')
if output: print(output)
raise SystemExit(code)
'''

scenarios = {
    'stop-fails': (False, 0, 0),
    'ps-fails': (False, 0, 0),
    'invalid-id': (False, 0, 0),
    'inspect-fails': (False, 0, 0),
    'still-running': (False, 0, 0),
    'invalid-state': (False, 0, 0),
    'second-running': (False, 0, 0),
    'stopped': (True, 1, 1),
    'no-existing-app': (True, 1, 1),
    'migration-fails': (False, 1, 0),
    'startup-fails': (False, 1, 1),
    'alerting-enabled': (True, 1, 1),
    'alerting-invalid': (False, 0, 0),
}

for scenario, (success, migrations, starts) in scenarios.items():
    with tempfile.TemporaryDirectory(prefix='deploy-quiescence-') as tmp:
        root = Path(tmp) / 'release'
        sha = '1' * 40
        staging = root / '.staging' / sha
        staging.mkdir(parents=True)
        required_files = [
            'docker-compose.yml', 'application-prod.yml', 'loki/loki-config.yaml',
            'alloy/config.alloy', 'alloy/docker-log-proxy.conf', 'alloy/docker-log-guard.js',
            'prometheus/prometheus.yml',
            'grafana/provisioning/datasources/datasource.yml',
            'scripts/compose-observability-diagnostics.sh',
            'scripts/backup-server-export.sh', 'scripts/backup-admin-audit.sh',
            'scripts/run-host-job.py', 'scripts/check-storage-health.py', 'scripts/backup-important-events.py',
            'systemd/host-jobs.env.example',
            'docker-compose.alerting.yml', 'alertmanager/alertmanager.yml',
            'scripts/validate-alerting-settings.py',
        ]
        for name in required_files:
            path = staging / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('fixture\n')
        (staging / 'src/main/resources/db/migration').mkdir(parents=True)
        app_env = staging / 'apps/server/.env'
        app_env.parent.mkdir(parents=True)
        app_env.write_text('''GOOGLE_CLIENT_ID=fixture
GOOGLE_CLIENT_SECRET=fixture
GOOGLE_REDIRECT_URI=https://www.zup-zup.com/api/login/oauth2/code/google
DISCORD_CLIENT_ID=fixture
DISCORD_CLIENT_SECRET=fixture
DISCORD_REDIRECT_URI=https://www.zup-zup.com/api/v1/users/discord/callback
''')
        smoke = staging / 'scripts/test-observability-smoke.sh'
        smoke.write_text('#!/bin/sh\nexit 0\n')
        smoke.chmod(0o755)
        validator = staging / 'scripts/validate-alerting-config.sh'
        validator.write_text('#!/bin/sh\nexit 0\n')
        (root / '.env').write_text('APP_IMAGE_NAME=ghcr.io/fixture/app\nFLYWAY_IMAGE=fixture@sha256:' + '0' * 64 + '\n')
        if scenario.startswith('alerting-'):
            enabled = 'true' if scenario == 'alerting-enabled' else 'invalid'
            with (root / '.env').open('a') as stream:
                stream.write(f'ALERTING_ENABLED={enabled}\n')
        fake_bin = Path(tmp) / 'bin'
        fake_bin.mkdir()
        curl_stub = '#!/bin/sh\nexit 0\n'
        for name, text in [('docker', docker_stub), ('curl', curl_stub)]:
            path = fake_bin / name
            path.write_text(text)
            path.chmod(0o755)
        fixture_script = Path(tmp) / 'deploy.sh'
        fixture_script.write_text(source.replace(fixed_root, f'readonly RELEASE_ROOT="{root}"'))
        trace = Path(tmp) / 'commands.jsonl'
        env = dict(os.environ, PATH=str(fake_bin) + os.pathsep + os.environ['PATH'],
                   SCENARIO=scenario, COMMAND_TRACE=str(trace))
        result = subprocess.run(['bash', str(fixture_script), sha, str(staging)],
                                env=env, text=True, capture_output=True, timeout=20)
        calls = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
        events = [call['event'] for call in calls]
        assert (result.returncode == 0) == success, (scenario, result.stderr)
        assert events.count('migrate') == migrations, (scenario, events)
        assert events.count('app-start') == starts, (scenario, events)
        if migrations and scenario != 'no-existing-app':
            assert events.index('inspect-app') < events.index('migrate'), (scenario, events)
        if scenario == 'alerting-invalid':
            assert 'ALERTING_ENABLED must be true or false' in result.stderr, result.stderr
        elif not migrations:
            assert 'migration was not started' in result.stderr, (scenario, result.stderr)
        if scenario == 'alerting-enabled':
            compose_calls = [call['args'] for call in calls if call['args'][:1] == ['compose']]
            assert compose_calls and all(any(arg.endswith('/docker-compose.alerting.yml') for arg in args)
                                         for args in compose_calls), compose_calls
            assert any('up' in args and 'alertmanager' in args for args in compose_calls), compose_calls
            assert all('--force-recreate' in args and '--no-deps' in args
                       for args in compose_calls if 'up' in args and 'alertmanager' in args), compose_calls
print(f'deploy quiescence contract passed: {len(scenarios)} isolated scenarios; no Docker/HTTP calls')
PY
