#!/bin/bash

# Stop Android Emulator Script
#
# This script stops any running Android emulator instances and related processes.
#
# Usage:
#   ./scripts/stop_emulator.sh

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_info "=========================================="
log_info "Stopping Android Emulator"
log_info "=========================================="

# Kill any running emulator processes
log_info "Killing emulator processes..."
pkill -f "emulator.*-avd" 2>/dev/null && log_info "Killed emulator processes" || log_warn "No emulator processes found"

# Kill any running install-start-emulator.sh scripts
log_info "Killing emulator startup scripts..."
pkill -f "install-start-emulator.sh" 2>/dev/null && log_info "Killed emulator startup scripts" || log_warn "No startup scripts found"

# Kill emulator via adb if device is connected
if adb devices 2>/dev/null | grep -qE "device$|emulator"; then
    log_info "Stopping emulator via adb..."
    adb emu kill 2>/dev/null || true
    sleep 2
else
    log_info "No emulator connected via adb"
fi

# Kill any remaining qemu processes (emulator backend)
log_info "Killing qemu processes..."
pkill -f qemu 2>/dev/null && log_info "Killed qemu processes" || log_warn "No qemu processes found"

log_info "=========================================="
log_info "Done"
log_info "=========================================="
