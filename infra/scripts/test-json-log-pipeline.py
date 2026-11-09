#!/usr/bin/env python3
"""Check JSON and legacy app logs through isolated Docker -> Alloy -> Loki."""
import datetime
import json
import os
import re
import subprocess
import tempfile
import time
import uuid
from pathlib import Path
from urllib.parse import urlencode

ROOT = Path(__file__).resolve().parents[2]


def docker(*args, check=True):
    return subprocess.run(["docker", *args], check=check, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30,
                          env={**os.environ, "APP_ENV_FILE": str(ROOT / "apps/server/.env.example")})


def pinned_images():
    compose = json.loads(docker("compose", "--env-file", str(ROOT / "infra/.env.example"),
                                "-f", str(ROOT / "infra/docker-compose.yml"),
                                "--profile", "observability", "config", "--format", "json").stdout)
    services = compose["services"]
    images = {name: services[name]["image"] for name in ("loki", "alloy", "observability-probe-tools")}
    assert all("@sha256:" in image for image in images.values()), "Use repository-pinned test images"
    return images


def write_fixtures(directory, marker, trace_id):
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    events = [
        {"@timestamp": timestamp, "level": "ERROR", "environment": "E2E", "event": "pipeline_failure",
         "message": "JSON exception event", "errorCode": "G001", "exceptionType": "IllegalStateException",
         "correlationId": trace_id, "stack_trace": "sanitized exception\n\tat app.Main.run(Main.java:1)"},
        {"@timestamp": timestamp, "level": "INFO", "environment": "E2E", "event": "pipeline_second",
         "message": "second independent JSON event", "correlationId": trace_id},
    ]
    legacy = f"2026-11-06 12:00:00.000 INFO [corr={trace_id}] legacy event"
    config = (ROOT / "infra/alloy/config.alloy").read_text()
    anchor = '\ttargets = discovery.docker.containers.targets\n'
    assert config.count(anchor) == 1
    # Limit discovery to the synthetic fixture; never ingest other local containers.
    config = config.replace(anchor, anchor + f'''
\trule {{
\t\tsource_labels = ["__meta_docker_container_label_zup_log_e2e"]
\t\tregex = "{marker}"
\t\taction = "keep"
\t}}
''')
    (directory / "config.alloy").write_text(config)
    (directory / "fixture.log").write_text("\n".join(json.dumps(e) for e in events) + "\n" + legacy + "\n")
    return events


def start_pipeline(prefix, marker, directory, images):
    docker("network", "create", "--internal", prefix)
    docker("run", "-d", "--rm", "--name", prefix + "-loki", "--network", prefix, "--network-alias", "loki",
           "--tmpfs", "/var/lib/loki:rw,uid=10001,gid=10001",
           "-v", f"{ROOT}/infra/loki/loki-config.yaml:/etc/loki/config.yaml:ro",
           images["loki"], "-config.file=/etc/loki/config.yaml")
    docker("run", "-d", "--rm", "--name", prefix + "-alloy", "--network", prefix,
           "-v", "/var/run/docker.sock:/var/run/docker.sock:ro",
           "-v", f"{directory}/config.alloy:/etc/alloy/config.alloy:ro",
           images["alloy"], "run", "/etc/alloy/config.alloy", "--storage.path=/tmp/alloy")
    docker("run", "-d", "--rm", "--name", prefix + "-fixture", "--network", prefix,
           "--label", "com.docker.compose.project=sugang-helper",
           "--label", "com.docker.compose.service=app", "--label", f"zup_log_e2e={marker}",
           "-v", f"{directory}/fixture.log:/fixture.log:ro", images["observability-probe-tools"],
           "sh", "-c", "sleep 6; cat /fixture.log; sleep 90")

    # 같은 테스트 표식이어도 다른 Compose 프로젝트의 로그는 수집하지 않아야 한다.
    docker("run", "-d", "--rm", "--name", prefix + "-excluded", "--network", prefix,
           "--label", "com.docker.compose.project=unrelated-project",
           "--label", "com.docker.compose.service=app", "--label", f"zup_log_e2e={marker}",
           images["observability-probe-tools"], "sh", "-c",
           "sleep 6; echo '2026-11-06 12:00:00 INFO excluded-project-sentinel'; sleep 90")


def query_logs(prefix, probe_image, expression):
    url = "http://loki:3100/loki/api/v1/query_range?" + urlencode({"query": expression, "limit": 100})
    result = docker("run", "--rm", "--name", prefix + "-probe", "--network", prefix,
                    probe_image, "wget", "-qO-", "-T", "3", url, check=False)
    if result.returncode:
        return []
    payload = json.loads(result.stdout)
    assert payload["status"] == "success", payload
    return payload["data"]["result"]


def wait_for_logs(prefix, probe_image, expression, minimum_events, deadline):
    records = []
    while time.monotonic() < deadline:
        records = query_logs(prefix, probe_image, expression)
        if sum(len(result["values"]) for result in records) >= minimum_events:
            return records
        time.sleep(1)
    return records


def verify_pipeline(prefix, probe_image, trace_id, events):
    dashboard = json.loads((ROOT / "infra/grafana/dashboards/loki-dashboard.json").read_text())
    trace_query = next(p["targets"][0]["expr"] for p in dashboard["panels"] if p["id"] == 4)
    trace_query = trace_query.replace("$job", "app").replace("${correlationId:regex}", re.escape(trace_id))
    stream = '{app="jbnu-sugang-helper",job="app"}'
    field_query = stream + f' | json | __error__="" | event="pipeline_failure" | errorCode="G001" | environment="E2E" | correlationId="{trace_id}"'
    deadline = time.monotonic() + 45
    found = wait_for_logs(prefix, probe_image, field_query, 1, deadline)
    assert found, "JSON fields were not independently searchable through the app pipeline"
    records = wait_for_logs(prefix, probe_image, stream, 3, deadline)
    lines = [line for result in records for _, line in result["values"]]
    decoded = [json.loads(line) for line in lines if line.startswith("{")]
    assert {e["event"] for e in decoded} == {e["event"] for e in events}, lines
    assert len(lines) == 3 and len(decoded) == 2, f"JSON events must not merge or split: {lines!r}"
    time.sleep(3)
    excluded = query_logs(prefix, probe_image, stream + ' |= "excluded-project-sentinel"')
    assert not excluded, "Logs from unrelated Compose projects must not be ingested"
    assert all("correlationId" not in result["stream"] for result in records), "Keep IDs out of ingestion labels"
    traced = [line for result in query_logs(prefix, probe_image, trace_query) for _, line in result["values"]]
    assert len(traced) == 3, "Grafana trace query must find both JSON events and the legacy event"
    print("JSON pipeline E2E passed: 2 JSON events, escaped stack, field search, legacy trace, no ID labels")


def main():
    marker = uuid.uuid4().hex[:12]
    prefix = f"zup-json-e2e-{marker}"
    images = pinned_images()
    trace_id = f"request.{marker}"
    try:
        with tempfile.TemporaryDirectory(prefix=prefix) as temporary:
            directory = Path(temporary)
            events = write_fixtures(directory, marker, trace_id)
            start_pipeline(prefix, marker, directory, images)
            verify_pipeline(prefix, images["observability-probe-tools"], trace_id, events)
    finally:
        for suffix in ("-fixture", "-excluded", "-alloy", "-loki", "-probe"):
            docker("rm", "-f", prefix + suffix, check=False)
        docker("network", "rm", prefix, check=False)


if __name__ == "__main__":
    main()
