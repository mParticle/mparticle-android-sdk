#!/usr/bin/env bash
#
# Reports Java -> Kotlin migration progress for the modules in scope of the
# short-term migration: the core SDK, the kit infrastructure and the Rokt kit.
#
# Only `src/main` is measured. Test source sets, sample apps and generated
# `build/` output are excluded so the number tracks shipped SDK code only.
#
# Files matched by scripts/kotlin-migration-facades.txt are Java files that are
# deliberately staying Java: those declaring public API, plus package-info.java,
# which has no Kotlin equivalent. They are counted separately so that "Java left
# to migrate" -- the number that must reach zero -- stays meaningful.
#
# Usage:
#   scripts/kotlin-migration-progress.sh [--root DIR] [--format markdown|env]
#
set -euo pipefail

ROOT="."
FORMAT="markdown"

while [[ $# -gt 0 ]]; do
    case "${1}" in
    --root)
        ROOT="${2}"
        shift 2
        ;;
    --format)
        FORMAT="${2}"
        shift 2
        ;;
    *)
        echo "unknown argument: ${1}" >&2
        exit 2
        ;;
    esac
done

MODULES=("android-core/src/main" "android-kit-base/src/main" "kits/rokt/rokt/src/main")
FACADE_LIST="${ROOT}/scripts/kotlin-migration-facades.txt"

# Glob patterns, relative to ROOT, for the Java files that stay Java.
facade_globs=()
if [[ -f ${FACADE_LIST} ]]; then
    while IFS= read -r line; do
        [[ -z ${line} ]] && continue
        [[ ${line} == \#* ]] && continue
        facade_globs+=("${line}")
    done <"${FACADE_LIST}"
fi

total_java=0
total_java_facade=0
total_kotlin=0
total_java_files=0
total_kotlin_files=0

rows=""
remaining_list=""

for module in "${MODULES[@]}"; do
    dir="${ROOT}/${module}"
    [[ -d ${dir} ]] || continue

    module_java=0
    module_java_facade=0
    module_kotlin=0
    module_java_files=0
    module_kotlin_files=0

    java_files=()
    # shellcheck disable=SC2312 # a find/sort failure yields an empty list, handled below
    while IFS= read -r file; do
        [[ -n ${file} ]] && java_files+=("${file}")
    done < <(find "${dir}" -name '*.java' -not -path '*/build/*' | sort)

    kotlin_files=()
    # shellcheck disable=SC2312 # a find/sort failure yields an empty list, handled below
    while IFS= read -r file; do
        [[ -n ${file} ]] && kotlin_files+=("${file}")
    done < <(find "${dir}" -name '*.kt' -not -path '*/build/*' | sort)

    for file in ${java_files[@]+"${java_files[@]}"}; do
        lines=$(wc -l <"${file}")
        lines="${lines//[[:space:]]/}"
        rel="${file#"${ROOT}"/}"

        is_facade=0
        for glob in ${facade_globs[@]+"${facade_globs[@]}"}; do
            # shellcheck disable=SC2254 # the entries are intentionally glob patterns
            case "${rel}" in
            ${glob})
                is_facade=1
                break
                ;;
            *) ;;
            esac
        done

        module_java=$((module_java + lines))
        module_java_files=$((module_java_files + 1))
        if [[ ${is_facade} -eq 1 ]]; then
            module_java_facade=$((module_java_facade + lines))
        else
            remaining_list+="${lines} ${rel}"$'\n'
        fi
    done

    for file in ${kotlin_files[@]+"${kotlin_files[@]}"}; do
        lines=$(wc -l <"${file}")
        lines="${lines//[[:space:]]/}"
        module_kotlin=$((module_kotlin + lines))
        module_kotlin_files=$((module_kotlin_files + 1))
    done

    module_remaining=$((module_java - module_java_facade))
    module_pct=$(awk -v k="${module_kotlin}" -v j="${module_java}" \
        'BEGIN { t = k + j; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')

    rows+="| \`${module%/src/main}\` | ${module_kotlin} | ${module_java} | ${module_java_facade} | **${module_remaining}** | ${module_pct}% |"$'\n'

    total_java=$((total_java + module_java))
    total_java_facade=$((total_java_facade + module_java_facade))
    total_kotlin=$((total_kotlin + module_kotlin))
    total_java_files=$((total_java_files + module_java_files))
    total_kotlin_files=$((total_kotlin_files + module_kotlin_files))
done

total_remaining=$((total_java - total_java_facade))
total_pct=$(awk -v k="${total_kotlin}" -v j="${total_java}" \
    'BEGIN { t = k + j; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')
goal_pct=$(awk -v k="${total_kotlin}" -v r="${total_remaining}" \
    'BEGIN { t = k + r; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')

if [[ ${FORMAT} == "env" ]]; then
    echo "KOTLIN_LOC=${total_kotlin}"
    echo "JAVA_LOC=${total_java}"
    echo "JAVA_FACADE_LOC=${total_java_facade}"
    echo "JAVA_REMAINING_LOC=${total_remaining}"
    echo "JAVA_FILES=${total_java_files}"
    echo "KOTLIN_FILES=${total_kotlin_files}"
    echo "KOTLIN_PCT=${total_pct}"
    echo "GOAL_PCT=${goal_pct}"
    exit 0
fi

cat <<EOF
| Module | Kotlin LOC | Java LOC | Java (facade, staying) | Java left to migrate | Kotlin % |
| --- | ---: | ---: | ---: | ---: | ---: |
${rows}| **Total** | **${total_kotlin}** | **${total_java}** | **${total_java_facade}** | **${total_remaining}** | **${total_pct}%** |

**Migration goal progress: ${goal_pct}%** (Kotlin LOC as a share of everything that is not a designated public-API facade.)

${total_java_files} Java files / ${total_kotlin_files} Kotlin files in scope.

<details><summary>Largest Java files still to migrate</summary>

| LOC | File |
| ---: | --- |
EOF

printf '%s' "${remaining_list}" | sort -rn | head -15 |
    awk '{ print "| " $1 " | `" $2 "` |" }'

echo
echo "</details>"
