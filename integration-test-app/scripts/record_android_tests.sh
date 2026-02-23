#!/bin/bash

# Android Integration Test Recorder
#
# This script records Android SDK requests to WireMock for later replay.
# It runs the integration test app against a live WireMock instance in recording mode.
#
# Usage:
#   ./scripts/record_android_tests.sh [options]
#
# Prerequisites:
#   - WireMock running in record mode
#   - Android emulator running
#   - MPARTICLE_API_KEY and MPARTICLE_API_SECRET environment variables set

set -e

# Get script directory and paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_ROOT="$(cd "${APP_DIR}/.." && pwd)"

# Source common functions
source "${SCRIPT_DIR}/common.sh"

# Parse arguments
WIREMOCK_TARGET_URL=${WIREMOCK_TARGET_URL:-"https://nativesdks.mparticle.com"}
SKIP_WIREMOCK=false
STOP_WIREMOCK=false

while [[ $# -gt 0 ]]; do
    case $1 in
    --skip-wiremock)
        SKIP_WIREMOCK=true
        shift
        ;;
    --stop-wiremock)
        STOP_WIREMOCK=true
        shift
        ;;
    --target-url)
        WIREMOCK_TARGET_URL="$2"
        shift 2
        ;;
    --api-key)
        export MPARTICLE_API_KEY="$2"
        shift 2
        ;;
    --api-secret)
        export MPARTICLE_API_SECRET="$2"
        shift 2
        ;;
    *)
        log_error "Unknown option: $1"
        echo "Usage: $0 [--skip-wiremock] [--stop-wiremock] [--target-url URL] [--api-key KEY] [--api-secret SECRET]"
        exit 1
        ;;
    esac
done

# Change to app directory
cd "${APP_DIR}"

log_info "------------------------------------------"
log_info "Android Integration Test Recorder"
log_info "------------------------------------------"

# Prompt for API credentials if not provided
if [[ -z ${MPARTICLE_API_KEY} ]] || [[ -z ${MPARTICLE_API_SECRET} ]]; then
    log_info "API credentials not provided. Please enter them:"

    if [[ -z ${MPARTICLE_API_KEY} ]]; then
        read -p "Enter mParticle API Key: " MPARTICLE_API_KEY
        export MPARTICLE_API_KEY
    fi

    if [[ -z ${MPARTICLE_API_SECRET} ]]; then
        read -p "Enter mParticle API Secret: " MPARTICLE_API_SECRET
        export MPARTICLE_API_SECRET
    fi

    if [[ -z ${MPARTICLE_API_KEY} ]] || [[ -z ${MPARTICLE_API_SECRET} ]]; then
        log_error "API credentials are required. Exiting."
        exit 1
    fi
fi

# Start WireMock (auto-start if not running)
if [[ ${SKIP_WIREMOCK} == false ]]; then
    if ! start_wiremock; then
        log_error "Failed to start WireMock. Exiting."
        exit 1
    fi
else
    log_info "Skipping WireMock startup (--skip-wiremock flag set)"
    if ! check_wiremock; then
        log_error "WireMock is not running. Please start it manually or remove --skip-wiremock flag."
        exit 1
    fi
fi

# Put WireMock into record mode
log_info "Configuring WireMock for recording..."
log_info "Target URL: ${WIREMOCK_TARGET_URL}"

# Configure WireMock to proxy and record
curl -X POST "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/recordings/start" \
    -H "Content-Type: application/json" \
    -d "{
        \"targetBaseUrl\": \"${WIREMOCK_TARGET_URL}\",
        \"extractHeaders\": {
            \"caseInsensitive\": true
        },
        \"requestBodyPattern\": {
            \"matcher\": \"equalToJson\",
            \"ignoreArrayOrder\": true,
            \"ignoreExtraElements\": true
        }
    }" >/dev/null 2>&1

log_info "WireMock recording started"

# Build and install app
build_sdk_and_app
start_emulator
install_app
set_env_vars

# Run test app
log_info "Running test app to record requests..."
run_test_app

# Stop recording and save mappings
log_info "Stopping WireMock recording..."
curl -X POST "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/recordings/stop" >/dev/null 2>&1

# Get recorded mappings
log_info "Retrieving recorded mappings..."
MAPPINGS_DIR="${PROJECT_ROOT}/wiremock/mappings/android"
mkdir -p "${MAPPINGS_DIR}"

curl -s "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/mappings" >"${MAPPINGS_DIR}/recorded-mappings.json"

log_info "Recordings saved to ${MAPPINGS_DIR}/recorded-mappings.json"
log_info "------------------------------------------"
log_info "Recording completed successfully!"
log_info "------------------------------------------"

# Stop WireMock if requested
if [[ ${STOP_WIREMOCK} == true ]]; then
    stop_wiremock
fi
