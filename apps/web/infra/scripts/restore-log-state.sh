#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: $0 <archive-path> [target-root]" >&2
  exit 1
fi

archive_path="$1"
target_root="${2:-/}"

if [ ! -f "${archive_path}" ]; then
  echo "archive not found: ${archive_path}" >&2
  exit 1
fi

mkdir -p "${target_root}"
tar -xzf "${archive_path}" -C "${target_root}"
