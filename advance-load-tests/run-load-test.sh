#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Advance Service Load Test ==="
echo "Target: ${ADVANCE_API_URL:-http://localhost:8080}"
echo "Keycloak: ${KEYCLOAK_URL:-http://localhost:8180}"
echo ""

# Override URLs if environment variables are set
EXTRA_PROPS=""
if [[ -n "${ADVANCE_API_URL:-}" ]]; then
    EXTRA_PROPS+=" -Dgatling.baseUrl=${ADVANCE_API_URL}"
fi

mvn gatling:test -f "${SCRIPT_DIR}/pom.xml" ${EXTRA_PROPS}

REPORT_DIR="${SCRIPT_DIR}/target/gatling"
LATEST=$(ls -td "${REPORT_DIR}"/advanceloadtest-* 2>/dev/null | head -1 || true)

if [[ -n "${LATEST}" ]]; then
    echo ""
    echo "=== Results: ${LATEST}/index.html ==="
else
    echo "No Gatling report found in ${REPORT_DIR}"
fi
