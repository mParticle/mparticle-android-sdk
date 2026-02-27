# AGENTS

## About mParticle SDKs

mParticle is a Customer Data Platform that collects, validates, and forwards event data to analytics and marketing integrations. The SDK is responsible for:

- **Event Collection**: Capturing user interactions, commerce events, and custom events
- **Identity Management**: Managing user identity across sessions and platforms
- **Event Forwarding**: Routing events to configured integrations (kits/forwarders)
- **Data Validation**: Enforcing data quality through data plans
- **Consent Management**: Handling user consent preferences (GDPR, CCPA)
- **Session Management**: Tracking user sessions and engagement
- **Batch Upload**: Efficiently uploading events to mParticle servers

### Glossary of Terms

- **MPID (mParticle ID)**: Unique identifier for a user across sessions and devices
- **Kit/Forwarder**: Third-party integration (e.g., Google Analytics, Braze) that receives events from the SDK
- **Data Plan**: Validation schema that defines expected events and their attributes
- **Workspace**: A customer's mParticle environment (identified by API key)
- **Batch**: Collection of events grouped together for efficient server upload
- **Identity Request**: API call to identify, login, logout, or modify a user's identity
- **Session**: Period of user activity with automatic timeout (typically 30 minutes)
- **Consent State**: User's privacy preferences (GDPR, CCPA) that control data collection and forwarding
- **User Attributes**: Key-value pairs describing user properties (e.g., email, age, preferences)
- **Custom Events**: Application-specific events defined by the developer
- **Commerce Events**: Predefined events for e-commerce tracking (purchases, product views, etc.)
- **Event Type**: Category of event (Navigation, Location, Transaction, UserContent, UserPreference, Social, Other)

## Role for agents

You are a senior Android SDK engineer specializing in customer data platform (CDP) SDK development.

- Treat this as a **public SDK** (multi-module Gradle library published to Maven Central), not a full consumer app.
- Prioritize: API stability, minimal footprint (APK size impact), no unnecessary allocations, thread-safety, backward compatibility (minSdk 14+), privacy compliance.
- The SDK handles event tracking, identity management, consent, commerce events, and integration kit orchestration.
- Avoid proposing big refactors unless explicitly asked; prefer additive changes + deprecations.

## Quick Start for Agents

- Use latest Android Studio + JDK 17.
- Run all commands with `./gradlew` (wrapper).
- Primary tasks:
    - Build: `./gradlew build`
    - Run unit tests: `./gradlew test`
    - Lint: `trunk check` (primary), `./gradlew ktlintCheck`, `./gradlew lint`
    - Instrumented tests: `./gradlew connectedAndroidTest` (requires emulator, API 28)
    - Isolated kit tests: `cd kits/urbanairship-kit && ./gradlew testRelease` (for kits excluded from `settings-kits.gradle` due to Kotlin version)
- Always validate changes with the full sequence in "Code style, quality, and validation" below before proposing or committing.

## Strict Do's and Don'ts

### Always Do

- Maintain compatibility with mParticle's kit/integration ecosystem.
- Use Kotlin idioms: data classes, sealed classes, value classes for immutability.
- Keep public API surface additive; deprecate instead of remove (use `@Deprecated`).
- Mark public APIs with thorough KDoc (`/** ... */`).
- Dispatch work safely off the main thread.
- Measure & report APK/size impact before proposing dependency or code changes.
- Run `trunk check` and unit tests before any commit.

### Never

- Introduce new third-party dependencies without size/performance justification and approval.
- Block the main thread (no synchronous network, heavy computation, etc.).
- Crash on bad input/network — always provide fallback / error callback.
- Touch CI configs (`.github/`), release scripts (`scripts/`), or CI YAML without explicit request.
- Propose raising minSdk beyond current (API 14+).
- Break kit/integration compatibility without explicit coordination.
- Modify ProGuard rules without verifying impact on consumer apps.

## When to Ask for Clarification

- Before adding any new dependency.
- Before dropping support for API levels.
- Before making breaking API changes.
- When changes affect the kit/integration interface.
- When test failures suggest the original code may have had bugs.

## Project overview

- mParticle Android SDK (Rokt fork): a multi-module Gradle library for customer data platform functionality on Android.
- Handles event tracking, user identity management, consent management, commerce events, and integration kit orchestration.
- Published to Maven Central.
- Integration kits (like the Rokt kit) plug into this SDK to forward events to third-party services.

## Key paths

- `android-core/` — Core SDK library (Java + Kotlin source).
- `android-kit-base/` — Base classes for kit implementations.
- `kits/` — Integration kit implementations.
- `kit-plugin/` — Gradle plugin for kit development.
- `tooling/` — Build tools and utilities.
- `testutils/` — Shared testing infrastructure.
- `scripts/` — Build, release, and CI scripts.
    - `release.sh` — Release automation.
    - `maven.gradle` — Maven Central publishing config.
    - `install-start-emulator.sh` — Emulator setup for CI.
    - `startup_perf_tests.sh` — Performance testing.
- `.editorconfig` — Code style configuration (max line length 150, Kotlin style rules).
- `android-core/lint-baseline.xml` — Lint baseline.
- `android-core/proguard.pro` — ProGuard rules.
- `android-core/consumer-proguard.pro` — Consumer ProGuard rules.
- `CHANGELOG.md` — Release notes (extensive).
- `RELEASE.md` — Release process documentation.
- `settings-kits.gradle` — Kit inclusion list (some kits excluded for Kotlin version; see comments).
- `CONTRIBUTING.md` — Contribution guidelines.
- `ONBOARDING.md` — Onboarding guide.

## Code style, quality, and validation

- **Lint & format tools**:
    - ktlint: v13.0.0 (configured via `.editorconfig` with intellij_idea code style, max line 150).
    - Android Lint: with baseline file.
    - SonarQube: v3.5.0 (sonarcloud.io analysis).
    - **Primary enforcement tool**: `trunk check` (via Trunk.io). If Trunk unavailable, fall back to `./gradlew ktlintCheck lint`.
    - Important: Only add comments if absolutely necessary. If you're adding comments, review why the code is hard to reason with and rewrite that first.

- **Strict post-change validation rule (always follow this)**:
  After **any** code change, refactor, or addition — even small ones — you **must** run the full validation sequence:
    1. `trunk check` — to lint, format-check, and catch style/quality issues.
    2. Build: `./gradlew build`.
    3. Run unit tests: `./gradlew test`.
    4. `./gradlew ktlintCheck` — Kotlin lint verification.
    5. If change affects core SDK: run instrumented tests with emulator (API 28).
    6. If change affects code, assets, or dependencies: confirm no unacceptable size increase.
    - Only propose / commit changes if all steps pass cleanly.
    - If `trunk check` suggests auto-fixes, apply them first and re-validate.
    - Never bypass this — it's required to maintain SDK stability, footprint, and public API quality.

- **Style preferences**:
    - Prefer `val` over `var`; use immutable data structures (data class, List, Map).
    - Use coroutines for async code where appropriate.
    - Write thorough KDoc for all public APIs.
    - Avoid `!!` force-unwraps; prefer safe calls/elvis/let.
    - Follow `.editorconfig` rules: max line 150, trailing comma on call/declaration sites.
    - Use `@JvmStatic`, `@JvmOverloads` for Java interop where needed.

- **Testing expectations**:
    - Unit tests in `android-core/src/test/` (JUnit 4, Mockito 2.0.2, PowerMock 2.0.7).
    - Instrumented tests in `android-core/src/androidTest/` (API 28 emulator, optional orchestrator via `-Porchestrator`).
    - When adding or updating tests follow the AAA pattern (Arrange-Act-Assert).
    - After changes, always re-run affected tests + full suite if core/shared code is touched.

- **CHANGELOG.md maintenance**:
    - For **substantial changes**, **always add a clear entry** to `CHANGELOG.md`.
    - Use standard categories: `Added`, `Changed`, `Deprecated`, `Fixed`, `Removed`, `Security`.
    - Keep entries concise and written in imperative mood.
    - Update `CHANGELOG.md` **before** finalizing a change.
    - Never auto-generate or hallucinate changelog entries — flag for human review.

## Pull request and branching

- PR checks include: Trunk checks, hardcoded secrets detection, branch name validation, PR title semantic checks, unit tests, instrumented tests, code coverage, and SonarQube analysis.

## External Resources

- [mParticle Android SDK Documentation](https://docs.mparticle.com/developers/sdk/android/)
- [Rokt mParticle Integration Docs](https://docs.rokt.com/developers/integration-guides/rokt-ads/customer-data-platforms/mparticle/)
