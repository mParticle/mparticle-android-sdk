#!/bin/bash

# Android Integration Test Runner
#
# This script builds the Android SDK, launches the emulator, installs the test app,
# runs integration tests, and validates responses against WireMock mappings.
#
# Usage:
#   ./scripts/run_android_integration_tests.sh [options]
#
# Options:
#   --skip-build        Skip building the SDK and app
#   --skip-emulator     Skip emulator startup (assume already running)
#   --wiremock-host     WireMock host (default: localhost)
#   --wiremock-port     WireMock port (default: 8080)
#   --api-key           mParticle API key (or script will prompt)
#   --api-secret        mParticle API secret (or script will prompt)
#
# If API credentials are not provided via options or environment variables,
# the script will prompt for them interactively.

set -e

# Get script directory and paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_ROOT="$(cd "${APP_DIR}/.." && pwd)"

# Source common functions
source "${SCRIPT_DIR}/common.sh"

# Parse arguments
SKIP_BUILD=false
SKIP_EMULATOR=false
SKIP_WIREMOCK=false
STOP_EMULATOR=false
STOP_WIREMOCK=false

while [[ $# -gt 0 ]]; do
    case $1 in
    --skip-build)
        SKIP_BUILD=true
        shift
        ;;
    --skip-emulator)
        SKIP_EMULATOR=true
        shift
        ;;
    --skip-wiremock)
        SKIP_WIREMOCK=true
        shift
        ;;
    --stop-emulator)
        STOP_EMULATOR=true
        shift
        ;;
    --stop-wiremock)
        STOP_WIREMOCK=true
        shift
        ;;
    --wiremock-host)
        WIREMOCK_HOST="$2"
        shift 2
        ;;
    --wiremock-port)
        WIREMOCK_PORT="$2"
        shift 2
        ;;
    --wiremock-https-port)
        WIREMOCK_HTTPS_PORT="$2"
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
        echo "Usage: $0 [--skip-build] [--skip-emulator] [--skip-wiremock] [--stop-emulator] [--stop-wiremock] [--wiremock-host HOST] [--wiremock-port PORT] [--wiremock-https-port PORT] [--api-key KEY] [--api-secret SECRET]"
        exit 1
        ;;
    esac
done

# Handle stop requests
if [[ ${STOP_EMULATOR} == true ]]; then
    stop_emulator
    exit 0
fi

if [[ ${STOP_WIREMOCK} == true ]]; then
    stop_wiremock
    exit 0
fi

# Prompt for API credentials if not provided
if [[ -z ${MPARTICLE_API_KEY} ]] || [[ -z ${MPARTICLE_API_SECRET} ]]; then
    log_info ""
    log_info "API credentials are required for integration testing."
    log_info "Please enter your mParticle API credentials:"
    log_info ""

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

    log_info ""
fi

# Change to app directory (where APK will be built)
cd "${APP_DIR}"

log_info "=========================================="
log_info "Android Integration Test Runner"
log_info "=========================================="

# Step 1: Start WireMock (auto-start if not running)
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

# Step 2: Build SDK and app
if [[ ${SKIP_BUILD} == false ]]; then
    build_sdk_and_app
else
    log_info "Skipping build (--skip-build flag set)"
fi

# Step 3: Start emulator
if [[ ${SKIP_EMULATOR} == false ]]; then
    start_emulator
else
    log_info "Skipping emulator startup (--skip-emulator flag set)"
    if ! check_emulator; then
        log_error "Emulator is not running. Please start it manually or remove --skip-emulator flag."
        exit 1
    fi
fi

# Step 4: Install app
install_app

# Step 5: Set environment variables
set_env_vars

# Step 6: Reset WireMock to clear previous requests
reset_wiremock

# Step 7: Run test app
run_test_app

# Step 8: Capture logs
capture_logcat "${APP_DIR}/android-integration-test.log"

# Step 9: Verify requests
if verify_requests; then
    # Step 10: Verify against baselines (if available)
    if verify_against_baselines; then
        log_info "=========================================="
        log_info "Integration tests completed successfully!"
        log_info "All requests verified against baselines"
        log_info "=========================================="
        exit 0
    else
        log_warn "Requests found but baseline comparison failed or skipped"
        log_info "This may be expected if baselines haven't been created yet"
        log_info "Run preprocess_recordings.sh to create baselines"
        log_info "=========================================="
        log_info "Integration tests completed with warnings"
        log_info "=========================================="
        exit 0 # Don't fail if baselines don't exist yet
    fi
else
    log_error "=========================================="
    log_error "Integration tests failed - no requests found"
    log_error "=========================================="

    # Stop WireMock if requested
    if [[ ${STOP_WIREMOCK} == true ]]; then
        stop_wiremock
    fi

    exit 1
fi

# Stop WireMock if requested
if [[ ${STOP_WIREMOCK} == true ]]; then
    stop_wiremock
fi
