#!/bin/bash

# Preprocessing Script for Android Integration Test Recordings
#
# This script extracts recorded requests from WireMock, normalizes dynamic fields,
# and prepares clean baseline files for comparison.
#
# Usage:
#   ./scripts/preprocess_recordings.sh [options]
#
# Options:
#   --input-file      Input recorded mappings file (default: wiremock/mappings/android/recorded-mappings.json)
#   --output-dir      Output directory for processed baselines (default: wiremock/mappings/android/baselines)
#   --dry-run         Show what would be done without making changes

set -e

# Get script directory and paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_ROOT="$(cd "${APP_DIR}/.." && pwd)"

# Source common functions
source "${SCRIPT_DIR}/common.sh"

# Default values
INPUT_FILE="${PROJECT_ROOT}/wiremock/mappings/android/recorded-mappings.json"
OUTPUT_DIR="${PROJECT_ROOT}/wiremock/mappings/android/baselines"
DRY_RUN=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
    --input-file)
        INPUT_FILE="$2"
        shift 2
        ;;
    --output-dir)
        OUTPUT_DIR="$2"
        shift 2
        ;;
    --dry-run)
        DRY_RUN=true
        shift
        ;;
    *)
        log_error "Unknown option: $1"
        echo "Usage: $0 [--input-file FILE] [--output-dir DIR] [--dry-run]"
        exit 1
        ;;
    esac
done

cd "${APP_DIR}"

log_info "=========================================="
log_info "Android Integration Test Preprocessor"
log_info "=========================================="

# Check if input file exists
if [[ ! -f ${INPUT_FILE} ]]; then
    log_error "Input file not found: ${INPUT_FILE}"
    log_info "Run record_android_tests.sh first to generate recordings"
    exit 1
fi

log_info "Input file: ${INPUT_FILE}"
log_info "Output directory: ${OUTPUT_DIR}"

if [[ ${DRY_RUN} == true ]]; then
    log_info "DRY RUN MODE - No files will be modified"
fi

# Create output directory
if [[ ${DRY_RUN} == false ]]; then
    mkdir -p "${OUTPUT_DIR}"
fi

# Check if python3 is available for JSON processing
if ! command -v python3 &>/dev/null; then
    log_error "python3 is required for preprocessing"
    log_info "Please install Python 3"
    exit 1
fi

# Process recordings using Python script
log_info "Processing recorded mappings..."

python3 <<'PYTHON_SCRIPT'
import json
import sys
import os
import re
from pathlib import Path

input_file = sys.argv[1]
output_dir = sys.argv[2]
dry_run = sys.argv[3] == "true"

# Dynamic fields to normalize/ignore
DYNAMIC_FIELDS = [
    "ts",      # Timestamp
    "ct",      # Client timestamp
    "id",      # Event ID
    "sid",     # Session ID
    "did",     # Device ID
    "iba",     # Instruction base address
    "el",      # Event length
    "mpid",    # mParticle ID (may vary)
]

def normalize_dynamic_fields(obj, path=""):
    """Recursively normalize dynamic fields in JSON object"""
    if isinstance(obj, dict):
        result = {}
        for key, value in obj.items():
            full_path = f"{path}.{key}" if path else key
            if key in DYNAMIC_FIELDS:
                # Replace with placeholder
                result[key] = "${json-unit.ignore}"
            else:
                result[key] = normalize_dynamic_fields(value, full_path)
        return result
    elif isinstance(obj, list):
        return [normalize_dynamic_fields(item, path) for item in obj]
    else:
        return obj

def extract_request_body(mapping):
    """Extract request body from WireMock mapping"""
    if "request" in mapping and "bodyPatterns" in mapping["request"]:
        for pattern in mapping["request"]["bodyPatterns"]:
            if "equalToJson" in pattern:
                return pattern["equalToJson"]
            elif "matchesJsonPath" in pattern:
                # For JSON path matches, we can't extract full body
                return None
    return None

def generate_baseline_name(mapping, index):
    """Generate a meaningful name for the baseline file"""
    # Try to extract event name or endpoint from mapping
    url_path = mapping.get("request", {}).get("urlPath", "")
    method = mapping.get("request", {}).get("method", "POST")
    
    # Extract endpoint name from URL
    if "/config" in url_path:
        return f"baseline-config-{index}.json"
    elif "/events" in url_path:
        return f"baseline-events-{index}.json"
    elif "/identity" in url_path or "/alias" in url_path:
        return f"baseline-identity-{index}.json"
    elif "/audience" in url_path:
        return f"baseline-audience-{index}.json"
    else:
        return f"baseline-{method.lower()}-{index}.json"

try:
    # Load recorded mappings
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    # Handle both single mapping and array of mappings
    mappings = data if isinstance(data, list) else [data]
    
    print(f"Found {len(mappings)} mapping(s) to process")
    
    processed_count = 0
    for i, mapping in enumerate(mappings):
        request_body = extract_request_body(mapping)
        if request_body:
            # Normalize dynamic fields
            normalized_body = normalize_dynamic_fields(request_body)
            
            # Generate baseline file name
            baseline_name = generate_baseline_name(mapping, i)
            output_path = os.path.join(output_dir, baseline_name)
            
            if dry_run:
                print(f"Would create: {output_path}")
                print(f"  Normalized {len([k for k in request_body.keys() if k in DYNAMIC_FIELDS])} dynamic fields")
            else:
                # Write normalized baseline
                with open(output_path, 'w') as f:
                    json.dump(normalized_body, f, indent=2)
                print(f"Created: {output_path}")
                processed_count += 1
        else:
            print(f"Mapping {i}: No request body found (may use JSON path matching)")
    
    if not dry_run:
        print(f"\nProcessed {processed_count} baseline(s)")
        print(f"Baselines saved to: {output_dir}")
    
except FileNotFoundError:
    print(f"Error: Input file not found: {input_file}", file=sys.stderr)
    sys.exit(1)
except json.JSONDecodeError as e:
    print(f"Error: Invalid JSON in input file: {e}", file=sys.stderr)
    sys.exit(1)
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
PYTHON_SCRIPT

if [[ $? -eq 0 ]]; then
    log_info "=========================================="
    log_info "Preprocessing completed successfully!"
    log_info "=========================================="
    exit 0
else
    log_error "Preprocessing failed"
    exit 1
fi
