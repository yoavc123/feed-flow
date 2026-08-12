#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

step() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
}

run_cmd() {
  printf '+ %s\n' "$*"
  "$@"
}

run_shell() {
  printf '+ %s\n' "$1"
  bash -lc "$1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

run_checks_job() {
  step "Checks job: refresh translations"
  run_cmd bash .scripts/refresh-translations.sh

  step "Checks job: detekt + allTests"
  run_cmd ./gradlew --quiet --console=plain detekt allTests
}

run_android_build_job() {
  step "Android build job: assemble debug app"
  run_cmd ./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug
}

if [[ $# -gt 0 ]]; then
  case "$1" in
    -h|--help)
      cat <<'EOF'
Usage: ./.scripts/ci.sh
EOF
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n' "$1" >&2
      exit 1
      ;;
  esac
fi

step "Running local CI workflow equivalent from .github/workflows/code-checks.yaml"
run_checks_job
run_android_build_job

step "Local CI workflow completed successfully"
