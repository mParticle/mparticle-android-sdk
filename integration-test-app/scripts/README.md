# Integration Test Scripts

This directory contains scripts for running Android SDK integration tests.

## Scripts

### `run_android_integration_tests.sh`

Main test runner that builds the SDK, launches the emulator, installs the test app, runs tests, and validates requests against WireMock.

**Usage:**

```bash
cd integration-test-app
./scripts/run_android_integration_tests.sh [options]
```

**Options:**

- `--skip-build` - Skip building the SDK and app
- `--skip-emulator` - Skip emulator startup (assume already running)
- `--wiremock-host HOST` - WireMock host (default: localhost)
- `--wiremock-port PORT` - WireMock port (default: 8080)
- `--api-key KEY` - mParticle API key (or set MPARTICLE_API_KEY env var)
- `--api-secret SECRET` - mParticle API secret (or set MPARTICLE_API_SECRET env var)

### `record_android_tests.sh`

Records Android SDK requests to WireMock for later replay. Requires API credentials.

**Usage:**

```bash
cd integration-test-app
./scripts/record_android_tests.sh [options]
```

**Options:**

- `--target-url URL` - Target URL for WireMock to proxy to (default: https://nativesdks.mparticle.com)
- `--api-key KEY` - mParticle API key (or set MPARTICLE_API_KEY env var)
- `--api-secret SECRET` - mParticle API secret (or set MPARTICLE_API_SECRET env var)

### `common.sh`

Shared functions used by both scripts. Should not be run directly.

## Prerequisites

- WireMock running (see main INTEGRATION_TESTING.md)
- Android emulator available
- `adb` command in PATH
- `curl` command available

## Examples

```bash
# Run tests with default settings
cd integration-test-app
./scripts/run_android_integration_tests.sh

# Run tests skipping build (faster if already built)
./scripts/run_android_integration_tests.sh --skip-build

# Record new test scenarios
export MPARTICLE_API_KEY="your-key"
export MPARTICLE_API_SECRET="your-secret"
./scripts/record_android_tests.sh
```

For detailed documentation, see [INTEGRATION_TESTING.md](../../INTEGRATION_TESTING.md).
