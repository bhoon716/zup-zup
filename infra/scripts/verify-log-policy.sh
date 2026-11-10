#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
"${script_dir}/test-runtime-contract.sh"
echo "실행 환경 로그의 용량 제한 정책을 검증했습니다."
