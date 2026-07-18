#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
"${script_dir}/test-runtime-contract.sh"
echo "bounded runtime log policy verification passed"
