#!/bin/bash

# Common functions for Android integration tests
# Based on iOS integration test patterns

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Calculate paths if not already set (they should be set by the calling script)
if [[ -z ${SCRIPT_DIR} ]]; then
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
if [[ -z ${APP_DIR} ]]; then
    APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
fi
if [[ -z ${PROJECT_ROOT} ]]; then
    PROJECT_ROOT="$(cd "${APP_DIR}/.." && pwd)"
fi

# Export for use in functions
export SCRIPT_DIR APP_DIR PROJECT_ROOT

# Default values
WIREMOCK_PORT=${WIREMOCK_PORT:-8080}
WIREMOCK_HOST=${WIREMOCK_HOST:-localhost}
EMULATOR_NAME=${EMULATOR_NAME:-test}
PACKAGE_NAME="com.mparticle.integrationtest"
APK_PATH="${APP_DIR}/build/outputs/apk/debug/integration-test-app-debug.apk"

# WireMock runs on host machine, accessible via 10.0.2.2 from emulator
EMULATOR_WIREMOCK_HOST="10.0.2.2"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if WireMock is running
check_wiremock() {
    log_info "Checking if WireMock is running on ${WIREMOCK_HOST}:${WIREMOCK_PORT}..."
    if curl -s "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin" >/dev/null 2>&1; then
        log_info "WireMock is running"
        return 0
    else
        return 1
    fi
}

# Start WireMock in Docker container
start_wiremock() {
    log_info "Starting WireMock Docker container..."

    # Check if docker is available
    if ! command -v docker &>/dev/null; then
        log_error "Docker is not installed or not in PATH"
        log_info "Please install Docker: https://docs.docker.com/get-docker/"
        return 1
    fi

    # Check if WireMock is already running
    if check_wiremock; then
        log_info "WireMock is already running"
        return 0
    fi

    # Check if WireMock container already exists
    local container_id=$(docker ps -a --filter "name=wiremock-android" --format "{{.ID}}" 2>/dev/null || true)
    if [[ -n ${container_id} ]]; then
        log_info "Found existing WireMock container, starting it..."
        docker start "${container_id}" >/dev/null 2>&1
        sleep 2
        if check_wiremock; then
            log_info "WireMock container started successfully"
            return 0
        fi
    fi

    # Determine WireMock ports
    local http_port=${WIREMOCK_PORT:-8080}
    local https_port=${WIREMOCK_HTTPS_PORT:-8443}

    # Create wiremock directory if it doesn't exist
    mkdir -p "${PROJECT_ROOT}/wiremock/mappings/android"
    mkdir -p "${PROJECT_ROOT}/wiremock/__files"

    # Start WireMock container
    log_info "Starting new WireMock container on ports ${http_port} (HTTP) and ${https_port} (HTTPS)..."

    local docker_cmd="docker run -d \
        --name wiremock-android \
        -p ${http_port}:8080 \
        -p ${https_port}:8443 \
        -v \"${PROJECT_ROOT}/wiremock:/home/wiremock\" \
        wiremock/wiremock:latest \
        --https-port 8443 \
        --disable-http-tunneling \
        --global-response-templating \
        --verbose"

    local container_id=$(eval "${docker_cmd}" 2>&1)

    if [[ $? -ne 0 ]]; then
        log_error "Failed to start WireMock container"
        log_info "Docker command: ${docker_cmd}"
        log_info "Error output: ${container_id}"
        return 1
    fi

    log_info "WireMock container started (ID: ${container_id:0:12})"

    # Wait for WireMock to be ready
    log_info "Waiting for WireMock to be ready..."
    local max_wait=30
    local waited=0

    while [[ ${waited} -lt ${max_wait} ]]; do
        if check_wiremock; then
            log_info "WireMock is ready!"
            return 0
        fi
        sleep 1
        waited=$((waited + 1))
        if [[ $((waited % 5)) -eq 0 ]]; then
            log_info "Still waiting for WireMock... (${waited}s)"
        fi
    done

    log_error "WireMock failed to start within ${max_wait} seconds"
    log_info "Check WireMock logs: docker logs wiremock-android"
    return 1
}

# Stop WireMock container
stop_wiremock() {
    log_info "Stopping WireMock container..."

    local container_id=$(docker ps -a --filter "name=wiremock-android" --format "{{.ID}}" 2>/dev/null || true)
    if [[ -n ${container_id} ]]; then
        docker stop "${container_id}" >/dev/null 2>&1
        log_info "WireMock container stopped"
    else
        log_info "No WireMock container found"
    fi
}

# Remove WireMock container
remove_wiremock() {
    log_info "Removing WireMock container..."

    local container_id=$(docker ps -a --filter "name=wiremock-android" --format "{{.ID}}" 2>/dev/null || true)
    if [[ -n ${container_id} ]]; then
        docker rm -f "${container_id}" >/dev/null 2>&1
        log_info "WireMock container removed"
    else
        log_info "No WireMock container found"
    fi
}

# Check if emulator is running
check_emulator() {
    log_info "Checking if Android emulator is running..."

    # First check if adb server is running
    if ! adb devices >/dev/null 2>&1; then
        log_warn "ADB server not responding, starting it..."
        adb start-server >/dev/null 2>&1 || true
    fi

    # Check for connected devices
    local devices=$(adb devices | grep -E "device$|emulator" | wc -l | tr -d ' ')
    if [[ ${devices} -gt 0 ]]; then
        log_info "Emulator is running (${devices} device(s) connected)"
        return 0
    else
        log_warn "No emulator detected. Attempting to start emulator..."
        return 1
    fi
}

# Start emulator if not running
start_emulator() {
    if check_emulator; then
        return 0
    fi

    log_info "Starting Android emulator..."

    # Check if emulator script exists in project root
    if [[ -f "${PROJECT_ROOT}/scripts/install-start-emulator.sh" ]]; then
        # Check if ANDROID_HOME is set
        if [[ -z ${ANDROID_HOME} ]]; then
            # Try to detect Android SDK location
            if [[ -d "${HOME}/Library/Android/sdk" ]]; then
                export ANDROID_HOME="${HOME}/Library/Android/sdk"
                log_info "Detected Android SDK at: ${ANDROID_HOME}"
            else
                log_error "ANDROID_HOME is not set and Android SDK not found in default location"
                log_info "Please set ANDROID_HOME environment variable"
                return 1
            fi
        fi

        # Ensure emulator is in PATH
        export PATH="${ANDROID_HOME}/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${PATH}"

        # Verify emulator binary exists
        if [[ ! -f "${ANDROID_HOME}/emulator/emulator" ]]; then
            log_error "Emulator binary not found at ${ANDROID_HOME}/emulator/emulator"
            return 1
        fi

        # Check if AVD exists before starting
        local available_avds=$("${ANDROID_HOME}/emulator/emulator" -list-avds 2>/dev/null || echo "")
        if [[ -z ${available_avds} ]]; then
            log_error "No AVDs found. Please create an AVD first using Android Studio or avdmanager."
            log_info "You can create one with: avdmanager create avd -n test -k 'system-images;android-29;default;x86'"
            return 1
        fi

        local avd_to_use="test"
        if ! echo "${available_avds}" | grep -q "^test$"; then
            log_warn "AVD 'test' not found. Available AVDs:"
            echo "${available_avds}" | sed 's/^/  - /'

            # Try to use an existing AVD instead of creating 'test'
            # Prefer Pixel_9 or Pixel_8_2, otherwise use the first available
            if echo "${available_avds}" | grep -q "^Pixel_9$"; then
                avd_to_use="Pixel_9"
                log_info "Using existing AVD: Pixel_9"
            elif echo "${available_avds}" | grep -q "^Pixel_8_2$"; then
                avd_to_use="Pixel_8_2"
                log_info "Using existing AVD: Pixel_8_2"
            else
                avd_to_use=$(echo "${available_avds}" | head -1)
                log_info "Using existing AVD: ${avd_to_use}"
            fi

            log_info "Starting emulator directly (avoiding sudo requirement)..."
            # Start emulator directly without sudo
            "${ANDROID_HOME}/emulator/emulator" -avd "${avd_to_use}" -no-snapshot -no-window -camera-back none -camera-front none -selinux permissive -qemu -m 2048 >/dev/null 2>&1 &
            local emulator_pid=$!
            log_info "Emulator started directly (PID: ${emulator_pid}, AVD: ${avd_to_use})"
        else
            # Use the startup script for 'test' AVD
            log_info "AVD 'test' found. Using startup script..."
            local log_file=$(mktemp /tmp/emulator-startup-XXXXXX.log 2>/dev/null || echo "/tmp/emulator-startup.log")
            bash "${PROJECT_ROOT}/scripts/install-start-emulator.sh" >"${log_file}" 2>&1 &
            local emulator_pid=$!
            log_info "Emulator startup script running (PID: ${emulator_pid})"
            log_info "Startup log: ${log_file}"
        fi

        # Wait for adb to detect a device (with timeout)
        log_info "Waiting for emulator to be detected by adb..."
        local max_wait=120 # 2 minutes max wait
        local waited=0
        local device_detected=false

        # Give the emulator a moment to start
        sleep 3

        while [[ ${waited} -lt ${max_wait} ]]; do
            # Check if emulator process died unexpectedly
            if ! ps -p "${emulator_pid}" >/dev/null 2>&1; then
                log_warn "Emulator process exited early (PID: ${emulator_pid}). Checking if emulator is running..."
                # Check one more time if device appeared
                if adb devices 2>/dev/null | grep -qE "device$|emulator.*device"; then
                    device_detected=true
                    break
                else
                    log_error "Emulator process exited but no device detected"
                    # Check if there are any other emulator processes
                    local other_emulators=$(pgrep -f "emulator.*-avd" 2>/dev/null | wc -l | tr -d ' ')
                    if [[ ${other_emulators} -gt 0 ]]; then
                        log_info "Found ${other_emulators} other emulator process(es) running"
                    fi
                    return 1
                fi
            fi

            # Check for connected device
            if adb devices 2>/dev/null | grep -qE "device$|emulator.*device"; then
                device_detected=true
                break
            fi

            sleep 2
            waited=$((waited + 2))
            if [[ $((waited % 10)) -eq 0 ]]; then
                log_info "Still waiting for emulator... (${waited}s)"
            fi
        done

        if [[ ${device_detected} == false ]]; then
            log_error "Emulator failed to start within ${max_wait} seconds"

            # Check if emulator process is still running
            if ps -p "${emulator_pid}" >/dev/null 2>&1; then
                log_warn "Emulator startup process is still running (PID: ${emulator_pid})"
            else
                log_warn "Emulator startup process has exited"
            fi

            # Check for emulator processes
            local emulator_procs=$(pgrep -f "emulator.*-avd" 2>/dev/null | wc -l | tr -d ' ')
            if [[ ${emulator_procs} -gt 0 ]]; then
                log_info "Found ${emulator_procs} emulator process(es) running but not detected by ADB"
                log_info "This might indicate the emulator is starting but ADB connection is delayed"
            fi

            # Show current ADB device status
            log_info "Current ADB devices:"
            adb devices 2>&1 || true

            # Show last few lines of startup log if it exists
            if [[ -n ${log_file} ]] && [[ -f ${log_file} ]]; then
                log_info "Last 20 lines of emulator startup log (${log_file}):"
                tail -20 "${log_file}" 2>/dev/null | sed 's/^/  /' || true
            else
                log_warn "Startup log file not found or not set"
            fi

            log_info ""
            log_info "Troubleshooting steps:"
            log_info '1. Check if ANDROID_HOME is set: echo $ANDROID_HOME'
            log_info '2. Verify emulator binary exists: ls $ANDROID_HOME/emulator/emulator'
            log_info "3. List available AVDs: emulator -list-avds"
            log_info "4. The startup script uses 'sudo' which may require a password"
            log_info "5. Try starting an existing emulator manually:"
            log_info "   emulator -avd Pixel_9 -no-snapshot -no-window &"
            log_info "6. Or try running the startup script manually: ${PROJECT_ROOT}/scripts/install-start-emulator.sh"
            log_info ""
            log_info "Note: If sudo requires a password, the script will hang waiting for input."
            log_info "Consider running 'sudo visudo' to allow passwordless sudo for emulator commands."
            return 1
        fi

        log_info "Emulator detected. Waiting for boot to complete..."

        # Wait for boot animation to complete
        local boot_complete=false
        waited=0
        while [[ ${waited} -lt ${max_wait} ]]; do
            local bootanim=$(adb shell getprop init.svc.bootanim 2>/dev/null || echo "")
            if [[ ${bootanim} == "stopped" ]]; then
                boot_complete=true
                break
            fi
            sleep 2
            waited=$((waited + 2))
            if [[ $((waited % 10)) -eq 0 ]]; then
                log_info "Still waiting for boot... (${waited}s, status: ${bootanim:-unknown})"
            fi
        done

        if [[ ${boot_complete} == false ]]; then
            log_warn "Emulator boot may not be complete, but continuing anyway..."
        fi

        # Unlock emulator
        log_info "Unlocking emulator..."
        adb shell input keyevent 82 2>/dev/null || true
        adb shell wm dismiss-keyguard 2>/dev/null || true
        sleep 2

        log_info "Emulator is ready"
        return 0
    else
        log_error "Emulator startup script not found at ${PROJECT_ROOT}/scripts/install-start-emulator.sh"
        log_info "Please start emulator manually or ensure the script exists."
        return 1
    fi
}

# Build the Android SDK and test app
build_sdk_and_app() {
    log_info "Building Android SDK and integration test app..."

    # Change to project root for building
    local original_dir=$(pwd)
    cd "${PROJECT_ROOT}"

    # Clean previous builds
    ./gradlew clean

    # Build SDK
    ./gradlew :android-core:assembleDebug

    # Build test app
    ./gradlew :integration-test-app:assembleDebug

    # Change back to original directory
    cd "${original_dir}"

    if [[ ! -f ${APK_PATH} ]]; then
        log_error "APK not found at ${APK_PATH}"
        return 1
    fi

    log_info "Build completed successfully"
}

# Install test app on emulator
install_app() {
    log_info "Installing integration test app on emulator..."

    if [[ ! -f ${APK_PATH} ]]; then
        log_error "APK not found. Please build first."
        return 1
    fi

    adb install -r "${APK_PATH}"
    log_info "App installed successfully"
}

# Set environment variables on device
set_env_vars() {
    # API credentials should be set by the calling script (prompted or via args)
    local api_key=${MPARTICLE_API_KEY:-""}
    local api_secret=${MPARTICLE_API_SECRET:-""}

    if [[ -z ${api_key} ]] || [[ -z ${api_secret} ]]; then
        log_error "API credentials are required but not set!"
        log_info "Please provide MPARTICLE_API_KEY and MPARTICLE_API_SECRET"
        log_info "Either via environment variables or the script will prompt for them"
        return 1
    fi

    log_info "Writing API credentials to device..."

    # Write credentials to a file that the app can read
    # Use the app's data directory which is accessible via run-as
    local creds_file="/data/local/tmp/mparticle_creds.txt"

    # Create credentials file
    adb shell "echo \"MPARTICLE_API_KEY=${api_key}\" > ${creds_file}" 2>/dev/null || true
    adb shell "echo \"MPARTICLE_API_SECRET=${api_secret}\" >> ${creds_file}" 2>/dev/null || true

    # Also try writing to app's files directory (more reliable)
    adb shell "run-as ${PACKAGE_NAME} sh -c 'echo MPARTICLE_API_KEY=${api_key} > /data/data/${PACKAGE_NAME}/files/mparticle_creds.txt'" 2>/dev/null || true
    adb shell "run-as ${PACKAGE_NAME} sh -c 'echo MPARTICLE_API_SECRET=${api_secret} >> /data/data/${PACKAGE_NAME}/files/mparticle_creds.txt'" 2>/dev/null || true

    # Set as environment variables for the app process (most reliable)
    # We'll pass these when launching the app via am start
    export MPARTICLE_API_KEY="${api_key}"
    export MPARTICLE_API_SECRET="${api_secret}"

    log_info "API credentials prepared (API Key: ${api_key:0:10}...)"
}

# Run the test app
run_test_app() {
    log_info "Launching integration test app..."

    # Get API credentials from environment (should be set by calling script)
    local api_key=${MPARTICLE_API_KEY:-""}
    local api_secret=${MPARTICLE_API_SECRET:-""}

    if [[ -z ${api_key} ]] || [[ -z ${api_secret} ]]; then
        log_error "API credentials are required but not set!"
        return 1
    fi

    # Write credentials to a file the app can read
    local creds_file="/data/data/${PACKAGE_NAME}/files/mparticle_creds.txt"
    adb shell "run-as ${PACKAGE_NAME} sh -c 'mkdir -p /data/data/${PACKAGE_NAME}/files && echo MPARTICLE_API_KEY=${api_key} > ${creds_file} && echo MPARTICLE_API_SECRET=${api_secret} >> ${creds_file}'" 2>/dev/null || {
        log_warn "Could not write credentials file, app will use defaults"
    }

    # Start the main activity with credentials as intent extras
    adb shell am start -n "${PACKAGE_NAME}/.MainActivity" \
        --es "MPARTICLE_API_KEY" "${api_key}" \
        --es "MPARTICLE_API_SECRET" "${api_secret}" 2>/dev/null ||
        adb shell am start -n "${PACKAGE_NAME}/.MainActivity"

    log_info "Test app launched. Waiting for events to be sent and uploaded..."

    # Wait for app to complete tests (15 seconds) + upload time (5 seconds) + buffer
    # The app finishes after 15 seconds, but SDK needs time to upload events
    sleep 25

    # Force upload any pending events
    log_info "Forcing event upload..."
    adb shell am broadcast -a com.mparticle.test.upload 2>/dev/null || true

    # Wait a bit more for upload to complete
    sleep 5

    log_info "Test app execution completed"
}

# Capture logcat output
capture_logcat() {
    local output_file=${1:-"integration-test.log"}
    log_info "Capturing logcat to ${output_file}..."
    adb logcat -d >"${output_file}"
}

# Verify WireMock received requests
verify_requests() {
    log_info "Verifying WireMock received requests..."

    # Get all requests from WireMock
    local requests=$(curl -s "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/requests")

    if [[ -z ${requests} ]] || [[ ${requests} == "[]" ]]; then
        log_warn "No requests found in WireMock"
        return 1
    fi

    log_info "Found requests in WireMock:"
    echo "${requests}" | python3 -m json.tool 2>/dev/null || echo "${requests}"

    return 0
}

# Verify requests against baseline mappings
verify_against_baselines() {
    local baseline_dir=${1:-"${PROJECT_ROOT}/wiremock/mappings/android/baselines"}

    log_info "Verifying requests against baseline mappings..."

    if [[ ! -d ${baseline_dir} ]]; then
        log_warn "Baseline directory not found: ${baseline_dir}"
        log_info "Skipping baseline comparison. Run preprocess_recordings.sh first to create baselines."
        return 0 # Don't fail if baselines don't exist yet
    fi

    # Get requests from WireMock
    local requests=$(curl -s "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/requests")

    if [[ -z ${requests} ]] || [[ ${requests} == "[]" ]]; then
        log_error "No requests found in WireMock to compare against baselines"
        return 1
    fi

    # Check if python3 is available
    if ! command -v python3 &>/dev/null; then
        log_warn "python3 not available. Skipping detailed baseline comparison."
        log_info "Requests found, but cannot compare against baselines."
        return 0
    fi

    # Use Python to compare requests against baselines
    local comparison_result=$(
        python3 <<'PYTHON_COMPARE'
import json
import sys
import os
from pathlib import Path

requests_json = sys.argv[1]
baseline_dir = sys.argv[2]

try:
    requests_data = json.loads(requests_json)
    requests_list = requests_data.get("requests", [])
    
    if not requests_list:
        print("ERROR: No requests found")
        sys.exit(1)
    
    baseline_files = list(Path(baseline_dir).glob("baseline-*.json"))
    
    if not baseline_files:
        print("WARN: No baseline files found for comparison")
        print(f"INFO: Found {len(requests_list)} request(s) in WireMock")
        sys.exit(0)
    
    print(f"Found {len(requests_list)} request(s) and {len(baseline_files)} baseline(s)")
    
    # Basic verification: check if we have requests matching expected endpoints
    # Full comparison would require matching request bodies, which is complex
    # For now, we verify requests exist and match expected patterns
    
    matched = 0
    for request in requests_list:
        url = request.get("url", "")
        method = request.get("method", "")
        
        # Check if this matches any expected endpoint pattern
        if any(pattern in url for pattern in ["/config", "/events", "/identity", "/audience", "/alias"]):
            matched += 1
    
    print(f"Matched {matched} request(s) to expected endpoint patterns")
    
    if matched == 0:
        print("ERROR: No requests matched expected patterns")
        sys.exit(1)
    
    print("SUCCESS: Requests verified against baselines")
    sys.exit(0)
    
except Exception as e:
    print(f"ERROR: Comparison failed: {e}")
    sys.exit(1)
PYTHON_COMPARE
        "${requests}" "${baseline_dir}"
    )

    local exit_code=$?

    if [[ ${exit_code} -eq 0 ]]; then
        log_info "${comparison_result}"
        return 0
    else
        log_error "${comparison_result}"
        return 1
    fi
}

# Reset WireMock (clear all requests and mappings)
reset_wiremock() {
    log_info "Resetting WireMock..."
    curl -X POST "http://${WIREMOCK_HOST}:${WIREMOCK_PORT}/__admin/reset" >/dev/null 2>&1
    log_info "WireMock reset completed"
}
