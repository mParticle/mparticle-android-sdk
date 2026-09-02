#!/usr/bin/env bash
#
# Reports Java -> Kotlin migration progress for the modules in scope of the
# short-term migration: the core SDK, the kit infrastructure, and the Rokt kit.
#
# Only `src/main` is measured. Test source sets, sample apps and generated
# `build/` output are excluded so the number tracks shipped SDK code only.
#
# Files listed in scripts/kotlin-migration-facades.txt are Java files that are
# deliberately staying Java (public API facades and package-info.java, which has
# no Kotlin equivalent). They are counted separately so that "internal Java LOC
# remaining" -- the number that must reach zero -- is meaningful.
#
# Usage:
#   scripts/kotlin-migration-progress.sh [--root DIR] [--format markdown|env]
#
set -euo pipefail

ROOT="."
FORMAT="markdown"

while [ $# -gt 0 ]; do
  case "$1" in
  --root)
    ROOT="$2"
    shift 2
    ;;
  --format)
    FORMAT="$2"
    shift 2
    ;;
  *)
    echo "unknown argument: $1" >&2
    exit 2
    ;;
  esac
done

MODULES="android-core/src/main android-kit-base/src/main kits/rokt/rokt/src/main"
FACADE_LIST="${ROOT}/scripts/kotlin-migration-facades.txt"

# Paths (relative to ROOT) of Java files that are staying Java by design.
facade_patterns() {
  if [ -f "$FACADE_LIST" ]; then
    grep -vE '^\s*(#|$)' "$FACADE_LIST" || true
  fi
}

is_facade() {
  # $1 = path relative to ROOT
  local path="$1" pattern
  while IFS= read -r pattern; do
    [ -z "$pattern" ] && continue
    # shellcheck disable=SC2254 # patterns are intentionally globs
    case "$path" in
    $pattern) return 0 ;;
    esac
  done <<EOF
$(facade_patterns)
EOF
  return 1
}

total_java=0
total_java_facade=0
total_kotlin=0
total_java_files=0
total_kotlin_files=0

rows=""

for module in $MODULES; do
  dir="${ROOT}/${module}"
  [ -d "$dir" ] || continue

  m_java=0
  m_java_facade=0
  m_kotlin=0
  m_java_files=0
  m_kotlin_files=0

  while IFS= read -r file; do
    [ -z "$file" ] && continue
    lines=$(wc -l <"$file" | tr -d ' ')
    rel="${file#"${ROOT}"/}"
    m_java=$((m_java + lines))
    m_java_files=$((m_java_files + 1))
    if is_facade "$rel"; then
      m_java_facade=$((m_java_facade + lines))
    fi
  done <<EOF
$(find "$dir" -name '*.java' -not -path '*/build/*' | sort)
EOF

  while IFS= read -r file; do
    [ -z "$file" ] && continue
    lines=$(wc -l <"$file" | tr -d ' ')
    m_kotlin=$((m_kotlin + lines))
    m_kotlin_files=$((m_kotlin_files + 1))
  done <<EOF
$(find "$dir" -name '*.kt' -not -path '*/build/*' | sort)
EOF

  m_remaining=$((m_java - m_java_facade))
  pct=$(awk -v k="$m_kotlin" -v j="$m_java" 'BEGIN { t = k + j; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')

  rows="${rows}| \`${module%/src/main}\` | ${m_kotlin} | ${m_java} | ${m_java_facade} | **${m_remaining}** | ${pct}% |
"

  total_java=$((total_java + m_java))
  total_java_facade=$((total_java_facade + m_java_facade))
  total_kotlin=$((total_kotlin + m_kotlin))
  total_java_files=$((total_java_files + m_java_files))
  total_kotlin_files=$((total_kotlin_files + m_kotlin_files))
done

total_remaining=$((total_java - total_java_facade))
total_pct=$(awk -v k="$total_kotlin" -v j="$total_java" 'BEGIN { t = k + j; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')
goal_pct=$(awk -v k="$total_kotlin" -v r="$total_remaining" 'BEGIN { t = k + r; if (t == 0) print "100.0"; else printf "%.1f", 100 * k / t }')

if [ "$FORMAT" = "env" ]; then
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
EOF

echo
echo "<details><summary>Largest Java files still to migrate</summary>"
echo
echo "| LOC | File |"
echo "| ---: | --- |"
for module in $MODULES; do
  dir="${ROOT}/${module}"
  [ -d "$dir" ] || continue
  while IFS= read -r file; do
    [ -z "$file" ] && continue
    rel="${file#"${ROOT}"/}"
    is_facade "$rel" && continue
    printf '%s\t%s\n' "$(wc -l <"$file" | tr -d ' ')" "$rel"
  done <<EOF
$(find "$dir" -name '*.java' -not -path '*/build/*')
EOF
done | sort -rn | head -15 | awk -F'\t' '{ print "| " $1 " | `" $2 "` |" }'
echo
echo "</details>"
