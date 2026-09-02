#!/usr/bin/env bash
#
# api-guard-selftest.sh — proves scripts/api-guard.sh actually detects a broken
# public API, and that it tells a frozen-contract break apart from a reviewable
# internal one. Mutations are applied to a COPY of the compile jar in a temp
# directory; the real build outputs and the committed baselines are never
# touched.
#
# ponytail: no `set -e` here — the point is to capture the guard's non-zero
# exit codes rather than let them abort this script.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GUARD="${SCRIPT_DIR}/api-guard.sh"
MODULE="android-core"

# A public class in a documented package: removing it must be a gate failure.
FROZEN_VICTIM="com/mparticle/AttributionError.class"
# A public class inside com.mparticle.internal that no kit compiles against:
# removing it must be reported, but as a reviewable internal change.
# shellcheck disable=SC2016 # `$` is a literal in the jar entry name, not an expansion
REVIEWED_VICTIM='com/mparticle/internal/ApplicationContextWrapper$MethodType.class'

WORKDIR=""
# shellcheck disable=SC2329 # invoked indirectly through `trap ... EXIT`
cleanup() { [[ -n ${WORKDIR} ]] && rm -rf "${WORKDIR}"; }
trap cleanup EXIT

fail=0

note() { echo "api-guard-selftest: $*"; }

# 1. The real, unmodified tree must PASS.
note "case 1 — clean tree must pass"
bash "${GUARD}" check --module "${MODULE}" >/dev/null
clean_status=$?
if [[ ${clean_status} -ne 0 ]]; then
    echo "  FAIL: clean check exited ${clean_status}, expected 0" >&2
    fail=1
else
    note "  ok"
fi

REAL_JAR="$(bash "${GUARD}" jar-path --module "${MODULE}" 2>/dev/null)"
if [[ ! -f ${REAL_JAR} ]]; then
    echo "api-guard-selftest: could not locate the compile jar" >&2
    exit 1
fi

WORKDIR="$(mktemp -d)"

mutate() {
    # $1 = jar entry to delete, $2 = destination jar
    cp "${REAL_JAR}" "$2"
    (cd "$(dirname "$2")" && zip -q -d "$(basename "$2")" "$1") >/dev/null 2>&1
}

# 2. Removing a documented public class must be a FROZEN failure (exit 2).
note "case 2 — removing ${FROZEN_VICTIM} must fail as a frozen contract"
frozen_jar="${WORKDIR}/frozen.jar"
mutate "${FROZEN_VICTIM}" "${frozen_jar}"
frozen_output="$(bash "${GUARD}" check --module "${MODULE}" --jar "${frozen_jar}" 2>&1)"
frozen_status=$?
if [[ ${frozen_status} -ne 2 ]]; then
    echo "  FAIL: expected exit 2, got ${frozen_status}" >&2
    fail=1
elif ! grep -q "FROZEN CONTRACT CHANGED" <<<"${frozen_output}"; then
    echo "  FAIL: output did not classify the change as a frozen contract" >&2
    fail=1
else
    note "  ok"
fi

# 3. Removing an unreferenced internal class must be reported, but as a
#    reviewable internal change (exit 1), not a frozen break.
note "case 3 — removing an internal-only class must be reviewable, not frozen"
reviewed_jar="${WORKDIR}/reviewed.jar"
mutate "${REVIEWED_VICTIM}" "${reviewed_jar}"
reviewed_output="$(bash "${GUARD}" check --module "${MODULE}" --jar "${reviewed_jar}" 2>&1)"
reviewed_status=$?
if [[ ${reviewed_status} -ne 1 ]]; then
    echo "  FAIL: expected exit 1, got ${reviewed_status}" >&2
    fail=1
elif grep -q "FROZEN CONTRACT CHANGED" <<<"${reviewed_output}"; then
    echo "  FAIL: an internal-only change was reported as a frozen break" >&2
    fail=1
elif ! grep -q "Internal implementation changed" <<<"${reviewed_output}"; then
    echo "  FAIL: output did not classify the change as internal" >&2
    fail=1
else
    note "  ok"
fi

if [[ ${fail} -eq 0 ]]; then
    echo "SELFTEST PASS"
    exit 0
fi
echo "SELFTEST FAIL"
exit 1
