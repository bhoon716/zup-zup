#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_dir="${repo_root}/infra"
compose_file="${compose_dir}/docker-compose.yml"
compose_env="${compose_dir}/.env.example"
temporary_dir="$(mktemp -d)"
config_file="${temporary_dir}/compose.json"

cleanup() {
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

APP_ENV_FILE="${repo_root}/apps/server/.env.example" docker compose \
  --env-file "${compose_env}" \
  -f "${compose_file}" \
  --profile observability \
  config --format json >"${config_file}"

python3 - "${config_file}" "${repo_root}" <<'PY'
import json
import re
import sys
from pathlib import Path

with open(sys.argv[1], encoding="utf-8") as handle:
    compose = json.load(handle)
repo_root = Path(sys.argv[2])
services = compose.get("services", {})

def fail(message):
    raise SystemExit(message)

def strip_alloy_comments(config):
    """Replace Alloy comments with whitespace while preserving string contents/newlines."""
    output = []
    index = 0
    in_string = False
    escaped = False
    line_comment = False
    block_comment = False
    while index < len(config):
        character = config[index]
        next_character = config[index + 1] if index + 1 < len(config) else ""
        if line_comment:
            if character == "\n":
                line_comment = False
                output.append(character)
            else:
                output.append(" ")
            index += 1
            continue
        if block_comment:
            if character == "*" and next_character == "/":
                output.extend((" ", " "))
                block_comment = False
                index += 2
            else:
                output.append("\n" if character == "\n" else " ")
                index += 1
            continue
        if in_string:
            output.append(character)
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == '"':
                in_string = False
            index += 1
            continue
        if character == '"':
            in_string = True
            output.append(character)
            index += 1
        elif character == "/" and next_character == "/":
            output.extend((" ", " "))
            line_comment = True
            index += 2
        elif character == "/" and next_character == "*":
            output.extend((" ", " "))
            block_comment = True
            index += 2
        else:
            output.append(character)
            index += 1
    return "".join(output)

def extract_alloy_component(config, component, name):
    marker = re.compile(
        r"(?m)^[ \t]*"
        + re.escape(component)
        + r"[ \t]+\""
        + re.escape(name)
        + r"\"[ \t]*\{"
    )
    match = marker.search(config)
    if match is None:
        return None
    opening_brace = config.find("{", match.start(), match.end())
    depth = 0
    in_string = False
    escaped = False
    for index in range(opening_brace, len(config)):
        character = config[index]
        if in_string:
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == '"':
                in_string = False
            continue
        if character == '"':
            in_string = True
        elif character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return config[opening_brace + 1:index]
    return None

def top_level_alloy_text(block):
    output = []
    depth = 0
    in_string = False
    escaped = False
    for character in block:
        if in_string:
            output.append("\n" if character == "\n" else " ")
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == '"':
                in_string = False
            continue
        if character == '"':
            in_string = True
            output.append('"' if depth == 0 else " ")
        elif character == "{":
            depth += 1
            output.append("\x00")
        elif character == "}":
            depth = max(depth - 1, 0)
            output.append("\x00")
        elif depth > 0:
            output.append("\n" if character == "\n" else " ")
        else:
            output.append(character)
    return "".join(output)

def is_alloy_reference_expression(expression, reference):
    compact = re.sub(r"[ \t\r\n]+", "", expression)
    while compact.startswith("(") and compact.endswith(")"):
        depth = 0
        outer_close = None
        for index, character in enumerate(compact):
            if character == "(":
                depth += 1
            elif character == ")":
                depth -= 1
                if depth == 0:
                    outer_close = index
                    break
        if outer_close != len(compact) - 1:
            break
        compact = compact[1:-1]
    return compact == reference

def has_alloy_target_assignment(block, reference):
    top_level_block = top_level_alloy_text(block)
    assignment_pattern = re.compile(
        rf"(?ms)^[ \t]*targets[ \t]*=[ \t\r\n]*"
        rf"(?P<expression>(?:[() \t\r\n]|{re.escape(reference)})+)[ \t]*$"
    )
    return any(
        is_alloy_reference_expression(match.group("expression"), reference)
        for match in assignment_pattern.finditer(top_level_block)
    )

def has_active_alloy_job_pipeline(config):
    cleaned_config = strip_alloy_comments(config)
    relabel_block = extract_alloy_component(cleaned_config, "discovery.relabel", "containers")
    source_block = extract_alloy_component(cleaned_config, "loki.source.docker", "containers")
    if relabel_block is None or source_block is None:
        return False
    if not has_alloy_target_assignment(relabel_block, "discovery.docker.containers.targets"):
        return False
    if not has_alloy_target_assignment(source_block, "discovery.relabel.containers.output"):
        return False
    return re.search(
        r'rule\s*\{[^}]*source_labels\s*=\s*\["__meta_docker_container_label_com_docker_compose_service"\][^}]*target_label\s*=\s*"job"',
        relabel_block,
        re.DOTALL,
    ) is not None

expected = {"app", "db", "redis", "loki", "alloy", "grafana", "prometheus"}
if set(services) != expected:
    fail(f"observability profile must contain app/db/redis/loki/alloy/grafana/prometheus: {sorted(services)}")

for name in ("loki", "alloy", "grafana", "prometheus"):
    if services[name].get("profiles") != ["observability"]:
        fail(f"{name} must be opt-in through the observability profile")
    if services[name].get("restart") != "unless-stopped":
        fail(f"{name} must use restart: unless-stopped")
    if "sugang-helper-runtime" not in services[name].get("networks", {}):
        fail(f"{name} must stay on the internal runtime network")
    if not services[name].get("healthcheck"):
        fail(f"{name} must define a healthcheck")

if not str(services["loki"].get("image", "")).startswith("grafana/loki@sha256:"):
    fail("Loki image must be digest pinned")
loki_mounts = {str(item.get("target")): item for item in services["loki"].get("volumes", [])}
if "/etc/loki/local-config.yaml" not in loki_mounts or "/var/lib/loki" not in loki_mounts:
    fail("Loki config and persistent data mounts are required")

prometheus = services["prometheus"]
if not str(prometheus.get("image", "")).startswith("prom/prometheus@sha256:"):
    fail("Prometheus image must be digest pinned")
if prometheus.get("ports"):
    fail("Prometheus must not publish a host port")
prometheus_mounts = {str(item.get("target")): item for item in prometheus.get("volumes", [])}
if "/etc/prometheus/prometheus.yml" not in prometheus_mounts or "/prometheus" not in prometheus_mounts:
    fail("Prometheus config and persistent data mounts are required")
prometheus_command = " ".join(str(item) for item in prometheus.get("command", []))
if "--config.file=/etc/prometheus/prometheus.yml" not in prometheus_command:
    fail("Prometheus must use the staged config file")
if "--storage.tsdb.path=/prometheus" not in prometheus_command:
    fail("Prometheus must use the persistent data path")
if not prometheus.get("healthcheck"):
    fail("Prometheus must define a healthcheck")
prometheus_config = (repo_root / "infra/prometheus/prometheus.yml").read_text(encoding="utf-8")
for required in ("job_name: sugang-helper-app", "metrics_path: /actuator/prometheus", "app:8081"):
    if required not in prometheus_config:
        fail(f"Prometheus config must scrape the app Actuator endpoint: {required}")

if not str(services["alloy"].get("image", "")).startswith("grafana/alloy@sha256:"):
    fail("Alloy image must be digest pinned")
alloy_mounts = {str(item.get("target")): item for item in services["alloy"].get("volumes", [])}
if "/etc/alloy/config.alloy" not in alloy_mounts:
    fail("Alloy config mount is required")
if alloy_mounts.get("/var/run/docker.sock", {}).get("read_only") is not True:
    fail("Alloy must read the Docker socket without write access")
alloy_config = (repo_root / "infra/alloy/config.alloy").read_text(encoding="utf-8")
if "loki.source.docker" not in alloy_config:
    fail("Alloy must collect Docker logs with loki.source.docker")
if "loki.write" not in alloy_config:
    fail("Alloy must write logs to Loki")
if not has_active_alloy_job_pipeline(alloy_config):
    fail("Alloy must map the Compose service label to job on the active Loki source pipeline")
decoy_only_alloy_config = """
discovery.relabel "containers" {
  targets = discovery.docker.containers.targets
}
discovery.relabel "decoy" {
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label = "job"
  }
}
loki.source.docker "containers" {
  targets = discovery.relabel.containers.output
}
"""
if has_active_alloy_job_pipeline(decoy_only_alloy_config):
    fail("Alloy contract must reject a disconnected decoy job relabel rule")
parenthesized_alloy_config = """
discovery.relabel "containers" {
  targets = (discovery.docker.containers.targets)
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label = "job"
  }
}
loki.source.docker "containers" {
  targets = (discovery.relabel.containers.output)
}
"""
if not has_active_alloy_job_pipeline(parenthesized_alloy_config):
    fail("Alloy contract must accept parenthesized active pipeline wiring")
multiline_alloy_config = """
discovery.relabel "containers" {
  targets =
    discovery.docker.containers.targets
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label = "job"
  }
}
loki.source.docker "containers" {
  targets =
    discovery.relabel.containers.output
}
"""
if not has_active_alloy_job_pipeline(multiline_alloy_config):
    fail("Alloy contract must accept multiline active pipeline wiring")
malformed_parenthesized_alloy_config = parenthesized_alloy_config.replace(
    "(discovery.docker.containers.targets)",
    "(discovery.docker.containers.targets",
).replace(
    "(discovery.relabel.containers.output)",
    "(discovery.relabel.containers.output",
)
if has_active_alloy_job_pipeline(malformed_parenthesized_alloy_config):
    fail("Alloy contract must reject unbalanced active pipeline wiring")
nested_object_alloy_config = """
discovery.relabel "containers" {
  targets = {
    bad = 1
  }
  discovery.docker.containers.targets
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label = "job"
  }
}
loki.source.docker "containers" {
  targets = discovery.relabel.containers.output
}
"""
if has_active_alloy_job_pipeline(nested_object_alloy_config):
    fail("Alloy contract must reject a nested object followed by a bare target reference")

loki_dashboard_path = repo_root / "infra/grafana/dashboards/loki-dashboard.json"
if not loki_dashboard_path.exists():
    fail("Grafana Loki dashboard is missing")
loki_dashboard_json = json.loads(loki_dashboard_path.read_text(encoding="utf-8"))
loki_variables = loki_dashboard_json.get("templating", {}).get("list", [])
job_variables = [
    variable
    for variable in loki_variables
    if variable.get("name") == "job"
    and "label_values(job)" in f"{variable.get('definition', '')} {variable.get('query', '')}"
]
if not job_variables:
    fail("Grafana Loki dashboard must define the job variable from the Loki job label")
loki_expressions = [
    str(target.get("expr", "")).strip()
    for panel in loki_dashboard_json.get("panels", [])
    for target in panel.get("targets", [])
]
if not any('{job=~"$job"}' in expression for expression in loki_expressions):
    fail("Grafana Loki dashboard targets must filter the Alloy job label")

if not str(services["grafana"].get("image", "")).startswith("grafana/grafana@sha256:"):
    fail("Grafana image must be digest pinned")
grafana_ports = services["grafana"].get("ports", [])
if not any(str(port.get("host_ip")) == "127.0.0.1" and str(port.get("target")) == "3000" for port in grafana_ports):
    fail("Grafana must be reachable only through localhost")
grafana_healthcheck = " ".join(str(item) for item in services["grafana"]["healthcheck"].get("test", []))
if "grep -Eq" not in grafana_healthcheck or "[[:space:]]*" not in grafana_healthcheck:
    fail("Grafana healthcheck must accept JSON whitespace around the database status")

datasource = (repo_root / "infra/grafana/provisioning/datasources/datasource.yml").read_text(encoding="utf-8")
if "type: loki" not in datasource or "isDefault: true" not in datasource:
    fail("Grafana must provision Loki as its default datasource")
if "type: prometheus" not in datasource or "url: http://prometheus:9090" not in datasource:
    fail("Grafana must provision the internal Prometheus datasource")
if services["grafana"].get("depends_on", {}).get("prometheus", {}).get("condition") != "service_healthy":
    fail("Grafana must wait for healthy Prometheus")

dashboard_path = repo_root / "infra/grafana/dashboards/application-metrics-dashboard.json"
if not dashboard_path.exists():
    fail("Grafana application metrics dashboard is missing")
dashboard = dashboard_path.read_text(encoding="utf-8")
for required in ("Prometheus", "http_server_requests_seconds_count", "jvm_memory_used_bytes"):
    if required not in dashboard:
        fail(f"application metrics dashboard is missing: {required}")

dashboard_json = json.loads(dashboard)
panel_ids = [panel.get("id") for panel in dashboard_json.get("panels", []) if panel.get("id") is not None]
panel_id_counts = {}
for panel_id in panel_ids:
    panel_id_counts[panel_id] = panel_id_counts.get(panel_id, 0) + 1
duplicate_panel_ids = sorted(panel_id for panel_id, count in panel_id_counts.items() if count > 1)
if duplicate_panel_ids:
    fail(f"dashboard contains duplicate panel IDs: {duplicate_panel_ids}")

domain_panel_ids = {7, 8, 14, 15, 16, 17, 18, 19}
domain_panels = {
    panel.get("id"): panel for panel in dashboard_json.get("panels", [])
    if panel.get("id") in domain_panel_ids
}
if set(domain_panels) != domain_panel_ids:
    fail(f"domain metrics dashboard panels are incomplete: {sorted(domain_panels)}")
domain_expressions = {
    panel_id: [str(target.get("expr", "")).strip() for target in panel.get("targets", [])]
    for panel_id, panel in domain_panels.items()
}

producer_contracts = (
    (7, "notification.outbox.dlq", "notification_outbox_dlq_total",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/notification/application/SeatNotificationOutboxProcessor.java"),
    (14, "notification.outbox.retry", "notification_outbox_retry_total",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/notification/application/SeatNotificationOutboxProcessor.java"),
    (14, "notification.outbox.claim_to_attempt", "notification_outbox_claim_to_attempt_seconds_count",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/notification/application/SeatNotificationOutboxProcessor.java"),
    (15, "notification.provider.latency", "notification_provider_latency_seconds_count",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/notification/infra/NotificationProviderResilience.java"),
    (8, "crawler.runs", "crawler_runs_total",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/crawling/application/CourseCrawlerService.java"),
    (19, "crawler.upstream.latency", "crawler_upstream_latency_seconds_count",
     repo_root / "apps/server/src/main/java/bhoon/sugang_helper/crawling/infra/JbnuCourseApiClient.java"),
)
for panel_id, producer_name, prometheus_family, producer_path in producer_contracts:
    producer_source = producer_path.read_text(encoding="utf-8")
    if f'"{producer_name}"' not in producer_source:
        fail(f"registered metric producer is missing: {producer_name}")
    if not any(prometheus_family in expression for expression in domain_expressions[panel_id]):
        fail(f"panel {panel_id} does not consume registered metric family: {prometheus_family}")

target_contracts = (
    (7, r"sum\(notification_outbox_dlq_total\)"),
    (8, r'sum\(crawler_runs_total\{status="STARTED"\}\)'),
    (14, r"sum\(rate\(notification_outbox_claim_to_attempt_seconds_count\[5m\]\)\)\s+by\s+\(channel\)"),
    (14, r"sum\(rate\(notification_outbox_retry_total\[5m\]\)\)\s+by\s+\(channel\)"),
    (14, r"sum\(rate\(notification_outbox_dlq_total\[5m\]\)\)\s+by\s+\(channel\)"),
    (15, r"sum\(rate\(notification_provider_latency_seconds_count\[5m\]\)\)\s+by\s+\(provider, outcome\)"),
    (16, r"\(sum\(rate\(notification_provider_latency_seconds_sum\[5m\]\)\)\s+by\s+\(provider, outcome\)\s+/\s+sum\(rate\(notification_provider_latency_seconds_count\[5m\]\)\)\s+by\s+\(provider, outcome\)\)\s+\*\s+1000"),
    (17, r'sum\(rate\(crawler_runs_total\{status="STARTED"\}\[5m\]\)\)'),
    (17, r'sum\(rate\(crawler_runs_total\{status="FAILED"\}\[5m\]\)\)'),
    (18, r"\(sum\(rate\(crawler_upstream_latency_seconds_sum\[5m\]\)\)\s+/\s+sum\(rate\(crawler_upstream_latency_seconds_count\[5m\]\)\)\)\s+\*\s+1000"),
    (19, r"sum\(rate\(crawler_upstream_latency_seconds_count\[1m\]\)\)"),
)
for panel_id, target_pattern in target_contracts:
    if not any(re.fullmatch(target_pattern, expression) for expression in domain_expressions[panel_id]):
        fail(f"panel {panel_id} is missing target-level PromQL contract: {target_pattern}")

for obsolete_metric in (
    "notification_outbox_processed_total",
    "notification_outbox_failed_total",
    "notification_provider_delivery_total",
    "notification_provider_delivery_seconds_bucket",
    "course_crawler_execution_total",
    "course_crawler_failure_total",
    "http_client_requests_seconds_bucket",
    "http_client_requests_seconds_count",
):
    if any(obsolete_metric in expression for expressions in domain_expressions.values() for expression in expressions):
        fail(f"dashboard still queries an unregistered domain metric: {obsolete_metric}")
if any("or vector(0)" in expression for expressions in domain_expressions.values() for expression in expressions):
    fail("domain dashboard must show No Data for an absent producer instead of synthetic zero")

all_panels = {
    panel.get("id"): panel
    for panel in dashboard_json.get("panels", [])
    if panel.get("id") is not None
}
system_panel_ids = {2, 21, 27, 31}
system_panels = {panel_id: all_panels[panel_id] for panel_id in system_panel_ids if panel_id in all_panels}
if set(system_panels) != system_panel_ids:
    fail(f"system metrics dashboard panels are incomplete: {sorted(system_panels)}")
if 28 in all_panels:
    fail("Spring Cache panel must not be provisioned without a cache producer")
system_expressions = {
    panel_id: [str(target.get("expr", "")).strip() for target in panel.get("targets", [])]
    for panel_id, panel in system_panels.items()
}
all_dashboard_expressions = [
    str(target.get("expr", "")).strip()
    for panel in dashboard_json.get("panels", [])
    for target in panel.get("targets", [])
]
system_target_contracts = (
    (2, r"\(sum\(rate\(http_server_requests_seconds_sum\[5m\]\)\)\s+/\s+sum\(rate\(http_server_requests_seconds_count\[5m\]\)\)\)\s+\*\s+1000"),
    (21, r"\(sum\(rate\(http_server_requests_seconds_sum\[5m\]\)\)\s+/\s+sum\(rate\(http_server_requests_seconds_count\[5m\]\)\)\)\s+\*\s+1000"),
    (21, r"max\(http_server_requests_seconds_max\)\s+\*\s+1000"),
    (27, r"\(sum\(rate\(hikaricp_connections_acquire_seconds_sum\[5m\]\)\)\s+/\s+sum\(rate\(hikaricp_connections_acquire_seconds_count\[5m\]\)\)\)\s+\*\s+1000"),
    (27, r"hikaricp_connections_acquire_seconds_max\s+\*\s+1000"),
    (31, r"process_files_open_files"),
    (31, r"process_files_max_files"),
)
for panel_id, target_pattern in system_target_contracts:
    if not any(re.fullmatch(target_pattern, expression) for expression in system_expressions[panel_id]):
        fail(f"panel {panel_id} is missing system metric PromQL contract: {target_pattern}")

for obsolete_system_metric in (
    "http_server_requests_seconds_bucket",
    "hikaricp_connections_acquire_seconds_avg",
    "cache_gets_total",
    "process_open_files",
    "process_max_files",
):
    if any(obsolete_system_metric in expression for expression in all_dashboard_expressions):
        fail(f"dashboard still queries an unregistered system metric: {obsolete_system_metric}")

compose_text = (repo_root / "infra/docker-compose.yml").read_text(encoding="utf-8")
if "promtail" in compose_text:
    fail("Promtail must not remain in the production Compose contract")
for obsolete_service in ("alertmanager", "cadvisor", "node_exporter"):
    if obsolete_service in compose_text:
        fail(f"unrequested observability service must not remain: {obsolete_service}")

for obsolete in (
    repo_root / "infra/alertmanager",
    repo_root / "infra/grafana/dashboards/jvm-dashboard.json",
    repo_root / "infra/grafana/dashboards/notification-slo-dashboard.json",
):
    if obsolete.exists():
        fail(f"obsolete observability file must be removed: {obsolete}")

print("observability Compose contract passed")
PY
