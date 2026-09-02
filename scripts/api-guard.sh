#!/usr/bin/env bash
#
# api-guard.sh — snapshot / diff the public JVM surface of the published
# android-core and android-kit-base artifacts (class declarations, method and
# field signatures, as a Java or Kotlin consumer sees them).
#
# The Java -> Kotlin migration promises no public API change. A Kotlin
# conversion can break that promise invisibly at the source level: a public
# field becomes getX()/setX(), a static becomes Foo.Companion.baz(), a class
# becomes final, a default argument adds a DefaultConstructorMarker overload.
# All of those are visible in the compiled signature, which is what this guard
# reads. It runs `javap` over the AGP compile jar -- the exact jar consumers
# compile against -- and never executes SDK code.
#
# Usage:
#   scripts/api-guard.sh snapshot [--module NAME] [--jar PATH]
#   scripts/api-guard.sh update   [--module NAME]
#   scripts/api-guard.sh check    [--module NAME] [--jar PATH]
#   scripts/api-guard.sh jar-path [--module NAME]
#
# See docs/kotlin-migration/PR-GATE.md for the baseline-update policy.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FROZEN_INTERNALS="${SCRIPT_DIR}/api-guard-frozen-internals.txt"

ALL_MODULES=("android-core" "android-kit-base")
JAR_REL="build/intermediates/compile_library_classes_jar/release/bundleLibCompileToJarRelease/classes.jar"

usage() {
    echo "Usage: $(basename "$0") {snapshot|update|check|jar-path} [--module NAME] [--jar PATH]" >&2
    exit 1
}

baseline_path() {
    echo "${REPO_ROOT}/$1/api/$1.api"
}

# Builds the AGP compile jar for a module and echoes its path.
build_jar() {
    local module="$1" jar
    local version
    version="$(head -n 1 "${REPO_ROOT}/VERSION")"
    jar="${REPO_ROOT}/${module}/${JAR_REL}"
    (
        cd "${REPO_ROOT}"
        export ORG_GRADLE_PROJECT_VERSION="${version}"
        export ORG_GRADLE_PROJECT_version="${version}"
        ./gradlew ":${module}:bundleLibCompileToJarRelease" --console=plain -q
    ) >&2
    if [[ ! -f ${jar} ]]; then
        echo "api-guard: compile jar not produced for ${module}: ${jar}" >&2
        exit 1
    fi
    echo "${jar}"
}

# Emits one sorted "<class>|CLASS|<decl>" / "<class>|MEMBER|<decl>" line per
# public declaration in the jar. Only public classes are recorded: a
# package-private or anonymous class is not something a consumer can name.
# BuildConfig and R are excluded because they carry per-build values.
snapshot_jar() {
    local jar="$1" classes
    classes="$(mktemp)"

    unzip -Z1 "${jar}" '*.class' |
        sed 's|/|.|g; s|\.class$||' |
        grep -vE '(^|\.)BuildConfig$' |
        grep -vE '(^|\.)R(\$[A-Za-z0-9_]+)?$' |
        sort >"${classes}"

    # javap accepts many class names at once; batch to keep the command line sane.
    xargs -n 80 javap -public -classpath "${jar}" <"${classes}" 2>/dev/null |
        awk '
      function trim(s) { gsub(/^[ \t]+|[ \t]+$/, "", s); return s }
      function squeeze(s) { gsub(/[ \t]+/, " ", s); return s }
      /^Compiled from/ { next }
      {
        line = trim(squeeze($0))
        if (line == "") next

        if (line ~ /\{$/) {
          # A class, interface, enum or record declaration.
          current = ""
          if (line !~ /^public /) next          # not consumer-visible
          decl = line
          sub(/[ \t]*\{$/, "", decl)
          rest = decl
          if (match(rest, /(class|interface|enum|record) /)) {
            rest = substr(rest, RSTART + RLENGTH)
          } else next
          name = rest
          sub(/[ <].*$/, "", name)
          if (name == "") next
          current = name
          print current "|CLASS|" decl
          next
        }
        if (line == "}") { current = ""; next }
        if (current != "" && line ~ /;$/) { print current "|MEMBER|" line }
      }
    ' | sort

    rm -f "${classes}"
}

# Classifies a set of changed class names as frozen (public contract) or
# reviewed (internal implementation). Frozen = anything outside
# com.mparticle.internal, plus the named internal kit contracts.
is_frozen_class() {
    local class="$1" entry
    if [[ ${class} != com.mparticle.internal.* ]]; then
        return 0
    fi
    if [[ -f ${FROZEN_INTERNALS} ]]; then
        while IFS= read -r entry; do
            # Strip trailing comments, then surrounding whitespace. Most entries
            # carry a `# N kit files` note; without this they never match.
            entry="${entry%%#*}"
            entry="${entry#"${entry%%[![:space:]]*}"}"
            entry="${entry%"${entry##*[![:space:]]}"}"
            [[ -z ${entry} ]] && continue
            # shellcheck disable=SC2254 # entries are intentionally glob patterns
            case "${class}" in
            ${entry}) return 0 ;;
            *) ;;
            esac
        done <"${FROZEN_INTERNALS}"
    fi
    return 1
}

cmd_snapshot() {
    local module="$1" jar="$2"
    if [[ -z ${jar} ]]; then
        jar="$(build_jar "${module}")"
    fi
    snapshot_jar "${jar}"
}

cmd_update() {
    local module="$1" baseline jar
    baseline="$(baseline_path "${module}")"
    mkdir -p "$(dirname "${baseline}")"
    jar="$(build_jar "${module}")"
    snapshot_jar "${jar}" >"${baseline}"
    local count
    count="$(wc -l <"${baseline}")"
    count="${count//[[:space:]]/}"
    echo "api-guard: baseline updated — ${baseline#"${REPO_ROOT}"/} (${count} declarations)"
}

cmd_check() {
    local module="$1" jar="$2" baseline current changed frozen_hits reviewed_hits status frozen_rc
    baseline="$(baseline_path "${module}")"
    if [[ ! -f ${baseline} ]]; then
        echo "api-guard: no baseline at ${baseline#"${REPO_ROOT}"/} — run 'api-guard.sh update' first" >&2
        return 1
    fi
    if [[ -z ${jar} ]]; then
        jar="$(build_jar "${module}")"
    fi

    current="$(mktemp)"
    snapshot_jar "${jar}" >"${current}"

    if diff -u "${baseline}" "${current}" >/dev/null; then
        echo "api-guard: ${module} PASSED — no public API diff"
        rm -f "${current}"
        return 0
    fi

    echo "api-guard: ${module} FAILED — public API diff detected"
    echo
    diff -u "${baseline}" "${current}" || true
    echo

    changed="$(mktemp)"
    diff "${baseline}" "${current}" |
        grep -E '^[<>]' |
        sed 's/^[<>] //' |
        cut -d'|' -f1 |
        sort -u >"${changed}"

    frozen_hits=""
    reviewed_hits=""
    while IFS= read -r class; do
        [[ -z ${class} ]] && continue
        set +e
        is_frozen_class "${class}"
        frozen_rc=$?
        set -e
        if [[ ${frozen_rc} -eq 0 ]]; then
            frozen_hits+="  ${class}"$'\n'
        else
            reviewed_hits+="  ${class}"$'\n'
        fi
    done <"${changed}"

    status=1
    if [[ -n ${frozen_hits} ]]; then
        echo "FROZEN CONTRACT CHANGED — this is a gate failure, not a baseline update:"
        printf '%s' "${frozen_hits}"
        echo
        status=2
    fi
    if [[ -n ${reviewed_hits} ]]; then
        echo "Internal implementation changed — allowed, but the baseline update needs"
        echo "the diff and a kit-usage audit in the PR description:"
        printf '%s' "${reviewed_hits}"
        echo
    fi

    rm -f "${current}" "${changed}"
    return "${status}"
}

COMMAND="${1-}"
[[ -z ${COMMAND} ]] && usage
shift || true

MODULE=""
JAR=""
while [[ $# -gt 0 ]]; do
    case "${1}" in
    --module)
        MODULE="${2}"
        shift 2
        ;;
    --jar)
        JAR="${2}"
        shift 2
        ;;
    *)
        echo "unknown argument: ${1}" >&2
        usage
        ;;
    esac
done

if [[ -n ${JAR} && -z ${MODULE} ]]; then
    echo "api-guard: --jar requires --module" >&2
    exit 1
fi

modules=("${ALL_MODULES[@]}")
if [[ -n ${MODULE} ]]; then
    modules=("${MODULE}")
fi

overall=0
for mod in "${modules[@]}"; do
    case "${COMMAND}" in
    snapshot) cmd_snapshot "${mod}" "${JAR}" ;;
    jar-path) build_jar "${mod}" ;;
    update) cmd_update "${mod}" ;;
    check)
        set +e
        cmd_check "${mod}" "${JAR}"
        rc=$?
        set -e
        if [[ ${rc} -gt ${overall} ]]; then
            overall=${rc}
        fi
        ;;
    *) usage ;;
    esac
done

exit "${overall}"
