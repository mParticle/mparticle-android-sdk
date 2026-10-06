# Integration tests

End-to-end checks of what the SDK puts on the wire. Each test drives the public API from a fresh
install, then compares every request the SDK sent to a local [WireMock](https://wiremock.org)
with a committed baseline in `src/androidTest/assets/baselines/`. A changed field, a new or
missing field, a changed type, or a different number of requests fails the test with a
path-by-path diff.

This exists so internal rewrites, in particular moving `android-core` from Java to Kotlin, can be
shown not to change any payload. The tests are written in Java on purpose, so they also prove the
public API is still callable from Java unchanged.

## Running

Needs a running emulator (WireMock is reached through the emulator's `10.0.2.2` host alias).

```sh
./gradlew -Pintegration.tests=true :integration-tests:connectedDebugAndroidTest
```

The module is only included with `-Pintegration.tests=true`, so normal `test`, `lint` and
`ktlintCheck` sweeps skip it. The build starts WireMock (a Gradle dependency, bound to
`127.0.0.1:18080` and `:18443`) before the tests and stops it afterwards. CI runs the same command
in the `instrumented-integration` job.

## How it works

- **No real backend, no real credentials.** Responses are stubbed in `wiremock/mappings/`, the API
  key is fake, and the identity stubs return fixed MPIDs.
- **TLS.** The SDK always uses HTTPS. The build generates a throwaway CA and server certificate
  (`generateWireMockCerts`; nothing private is committed), and
  `res/xml/network_security_config.xml` trusts that CA for `10.0.2.2` only. Pinning is disabled
  through `NetworkOptions` in the test app only. Nothing in the shipped SDK changes.
- **Isolation.** The orchestrator clears app data between tests, so every scenario starts from a
  first launch. Uploads happen only when the test calls `upload()`, which keeps batching
  deterministic.
- **Normalisation** (`RequestNormalizer`). Values that differ from run to run or device to device
  (timestamps, device info, memory and battery state, the SDK version) become `<ignored>`, but
  their keys are kept. Generated IDs become `<id:N>`, numbered by first appearance, so the
  baseline still asserts which IDs must be equal (for example, each message's `sid` is the
  session-start `id`). Rules are scoped by JSON path, not by key name.

## Updating baselines

Only re-record for an intentional payload change, and review the baseline diff like code:

```sh
./gradlew -Pintegration.tests=true -Pintegration.tests.record=true :integration-tests:connectedDebugAndroidTest
git diff integration-tests/src/androidTest/assets/baselines
```

A new scenario fails until its baseline is recorded. If a value is genuinely nondeterministic,
add its path to `IGNORED_PATHS` or `ID_PATHS` in `RequestNormalizer` rather than editing the
baseline by hand.
