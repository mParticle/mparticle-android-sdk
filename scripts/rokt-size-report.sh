#!/usr/bin/env bash
# Measures what the Rokt SDK+ umbrella (com.rokt:rokt-sdk-plus -- Core SDK + Rokt kit + Rokt
# SDK + payment extension) adds to a minified release APK, alongside the Core SDK on its own.
#
# The umbrella can only be measured against published artifacts: the kit plugin wires kits to
# module coordinates rather than projects, and the Rokt SDK needs a newer AGP than the shared
# build. So this publishes the chain to mavenLocal, then builds the standalone size-report-rokt
# fixture (own wrapper/AGP) against it.
#
# Prints one line of JSON on stdout; everything else goes to stderr.
#
# Usage: rokt-size-report.sh [repo-root] [--skip-build]
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

FLAVORS=(baseline kit sdkplus)
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

# Base and head are published to the same mavenLocal, so they must not share a version: with an
# identical coordinate Gradle is free to serve the artifact it already resolved and both sides
# would report the same numbers. A per-checkout version makes that impossible.
publish_version() {
    local sha
    sha="$(git -C "${REPO_ROOT}" rev-parse --short HEAD)"
    echo "0.0.0-size-${sha}"
}

build() {
    local version sha
    version="$(publish_version)"
    sha="${MP_GIT_SHA:-$(git -C "${REPO_ROOT}" rev-parse HEAD)}"

    # cd, never `-p`: android-core's build.gradle shells out to `git rev-parse` via
    # String.execute(), which inherits the *process* working directory. Run from anywhere
    # without a .git and it silently falls through to MP_GIT_SHA, which NPEs when unset.
    cd "${REPO_ROOT}"
    export MP_GIT_SHA="${sha}"

    log "Publishing com.mparticle:*:${version} to mavenLocal..."
    ./gradlew publishMavenPublicationToMavenLocal -PVERSION="${version}" --no-daemon --quiet >&2

    log "Publishing com.mparticle:android-rokt-kit:${version} to mavenLocal..."
    # settings-kits.gradle renames the project paths, hence :kits:android-rokt:rokt.
    ./gradlew -c settings-kits.gradle \
        :kits:android-rokt:rokt:publishMavenPublicationToMavenLocal \
        -PVERSION="${version}" \
        -Pmparticle.kit.mparticleFromMavenLocalOnly=true --no-daemon --quiet >&2

    log "Publishing com.rokt:rokt-sdk-plus:${version} to mavenLocal..."
    ./gradlew -c settings-rokt-sdk-plus.gradle \
        :rokt-sdk-plus:publishMavenPublicationToMavenLocal \
        -PVERSION="${version}" --no-daemon --quiet >&2

    log "Assembling size-report-rokt fixture (${FLAVORS[*]})..."
    (
        cd "${REPO_ROOT}/size-report-rokt"
        ./gradlew assembleRelease -PmparticleVersion="${version}" --no-daemon --quiet
    ) >&2
}

# AGP writes output-metadata.json beside the APK. Reading it instead of globbing survives the
# `-unsigned` suffix this build produces, and any future rename.
apk_path() {
    local flavor="$1"
    local dir="${REPO_ROOT}/size-report-rokt/build/outputs/apk/${flavor}/release"
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

# unzip -v columns: Length Method Size Cmpr ... Name. $3 is the compressed size, the closest
# portable proxy for what a user downloads.
download_bytes() { unzip -v "$1" | awk '$1 ~ /^[0-9]+$/ { s += $3 } END { print s + 0 }'; }

# unzip -l columns: Length Date Time Name. Uncompressed dex bytes = the code-only impact.
dex_bytes() { unzip -l "$1" | awk '$4 ~ /\.dex$/ { s += $1 } END { print s + 0 }'; }

main() {
    [[ ${SKIP_BUILD} == true ]] || build

    local json="{" first=true
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
        log "${flavor}: ${install} bytes installed"
    done
    echo "${json}}"
}

main
