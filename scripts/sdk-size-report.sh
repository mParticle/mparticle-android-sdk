#!/usr/bin/env bash
# Measures what the mParticle Core SDK adds to a minified release APK. Builds both flavors of
# the size-report fixture from one toolchain and prints one line of JSON on stdout; everything
# else goes to stderr so stdout stays parseable.
#
# Usage: sdk-size-report.sh [repo-root] [--skip-build]
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

FLAVORS=(baseline core)
SKIP_BUILD=false
REPO_ROOT_ARG=""

for arg in "$@"; do
    case "${arg}" in
    --skip-build) SKIP_BUILD=true ;;
    -*)
        echo "Unknown option: ${arg}" >&2
        exit 1
        ;;
    *) REPO_ROOT_ARG="${arg}" ;;
    esac
done

REPO_ROOT="$(cd -- "${REPO_ROOT_ARG:-${SCRIPT_DIR}/..}" &>/dev/null && pwd)"

log() { echo "$*" >&2; }

build() {
    log "Assembling size-report fixture (${FLAVORS[*]})..."
    # cd, never `-p`: android-core's build.gradle shells out to `git rev-parse` via
    # String.execute(), which inherits the *process* working directory. Run from anywhere
    # without a .git and it silently falls through to MP_GIT_SHA, which NPEs when unset.
    local sha
    sha="${MP_GIT_SHA-}"
    if [[ -z ${sha} ]]; then
        sha="$(git -C "${REPO_ROOT}" rev-parse HEAD)"
    fi
    (
        cd "${REPO_ROOT}"
        MP_GIT_SHA="${sha}" \
            ./gradlew -Psize.report=true :size-report:assembleRelease --no-daemon --quiet
    ) >&2
}

# AGP writes output-metadata.json beside the APK. Reading it instead of globbing survives the
# `-unsigned` suffix, an archivesName change and any future second flavor dimension.
apk_path() {
    local flavor="$1"
    local dir="${REPO_ROOT}/size-report/build/outputs/apk/${flavor}/release"
    local metadata="${dir}/output-metadata.json"
    if [[ ! -f ${metadata} ]]; then
        log "ERROR: no output-metadata.json for flavor '${flavor}' at ${dir}"
        return 1
    fi
    local file_name
    file_name="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["elements"][0]["outputFile"])' "${metadata}")"
    echo "${dir}/${file_name}"
}

install_bytes() { wc -c <"$1" | tr -d ' '; }

# unzip -v columns: Length Method Size Cmpr ... Name. $3 is the compressed size, which is the
# closest portable proxy for what a user downloads.
# The trailing totals line also starts with a number, but its $3 is the compression
# percentage ("47%"), which awk would coerce to 47 and add to the sum.
download_bytes() {
    unzip -v "$1" | awk '$1 ~ /^[0-9]+$/ && $3 ~ /^[0-9]+$/ { s += $3 } END { print s + 0 }'
}

# unzip -l columns: Length Date Time Name. Uncompressed dex bytes = the code-only impact.
dex_bytes() { unzip -l "$1" | awk '$4 ~ /\.dex$/ { s += $1 } END { print s + 0 }'; }

# A fixture that silently stops linking the SDK still produces a perfectly valid APK, and the
# report would present that as a large size *saving*. Fail closed instead: the workflow leaves
# the JSON empty and the comment reads "not measured".
MAX_BASELINE_BYTES=$((128 * 1024))
MIN_CORE_OVER_BASELINE_BYTES=$((100 * 1024))

assert_plausible() {
    local baseline="$1" core="$2"
    local ok=true

    if ((baseline > MAX_BASELINE_BYTES)); then
        log "IMPLAUSIBLE: baseline APK is ${baseline} bytes, expected under ${MAX_BASELINE_BYTES}."
        log "  The baseline flavor should depend on nothing -- check its dependencies."
        ok=false
    fi
    if ((core - baseline < MIN_CORE_OVER_BASELINE_BYTES)); then
        log "IMPLAUSIBLE: the Core SDK adds $((core - baseline)) bytes, expected at least ${MIN_CORE_OVER_BASELINE_BYTES}."
        log "  R8 has probably stripped it -- check that the core fixture still starts MParticle."
        ok=false
    fi

    [[ ${ok} == true ]]
}

main() {
    [[ ${SKIP_BUILD} == true ]] || build

    local json="{" first=true
    local install_baseline=0 install_core=0
    for flavor in "${FLAVORS[@]}"; do
        local apk install download dex
        apk="$(apk_path "${flavor}")"
        install="$(install_bytes "${apk}")"
        download="$(download_bytes "${apk}")"
        dex="$(dex_bytes "${apk}")"
        [[ ${first} == true ]] || json+=","
        first=false
        json+="\"${flavor}_install_bytes\":${install}"
        json+=",\"${flavor}_download_bytes\":${download}"
        json+=",\"${flavor}_dex_bytes\":${dex}"
        case "${flavor}" in
        baseline) install_baseline="${install}" ;;
        core) install_core="${install}" ;;
        *)
            log "Unknown flavor: ${flavor}"
            return 1
            ;;
        esac
        log "${flavor}: ${install} bytes installed"
    done
    # Before emitting, not after: a failure here must leave stdout empty so the report
    # reads "not measured" instead of showing a fabricated saving.
    assert_plausible "${install_baseline}" "${install_core}"

    echo "${json}}"
}

main
