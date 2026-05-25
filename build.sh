#!/usr/bin/env bash
# Build script for Phantom AntiCheat (bash)
# Usage: ./build.sh [--skip-tests]

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

MAVEN_ARGS=(clean package -U)
SKIP_TESTS=false
if [ "${1:-}" = "--skip-tests" ] || [ "${SKIP_TESTS}" = "true" ]; then
  MAVEN_ARGS+=("-DskipTests")
fi

echo "[build] Running: mvn ${MAVEN_ARGS[*]}"
mvn "${MAVEN_ARGS[@]}"

echo "[build] Success. Artifacts are in: $DIR/target/"
