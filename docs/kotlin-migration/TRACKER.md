# Java to Kotlin migration tracker

Status of every pull request in the migration, in the order they are expected to land. Rules and
checks are in [PLAYBOOK.md](PLAYBOOK.md). Line counts are tracked-file line counts at the start of
the migration (`main` at `4ee3ba4c`, version 6.1.2); "Tests" is the number of existing test classes
named after the file, where 0 means a characterization test comes first.

Mark a row done by replacing the box with a link to the merged pull request.

## Baseline

| Module             | Java files | Java LOC | Kotlin files | Kotlin LOC |
| ------------------ | ---------: | -------: | -----------: | ---------: |
| `android-core`     |        120 |   22,457 |           32 |      3,238 |
| `android-kit-base` |         12 |    5,380 |            4 |        557 |

Java that converts: **16,550 LOC**. Java that stays by design (public facade, kit ABI and `package-info.java`, 65 files):
**11,287 LOC**.

## Phase 0 · Guardrails

No product code. Risk class L throughout.

| Done | PR  | Title                                                                               | Notes                                                                   |
| ---- | --- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| [ ]  | 0.1 | `ci: report binary compatibility against the last published release`                | `scripts/api_compat_report.py`; job **Binary Compatibility**            |
| [ ]  | 0.2 | `build: add public API dumps with binary-compatibility-validator`                   | `./gradlew apiCheck` in the Unit Tests job; `scripts/check_api_dump.py` |
| [ ]  | 0.3 | `test: add Java and Kotlin consumer fixtures built against the published artifacts` | `settings-compat.gradle`, `compat/`                                     |
| [ ]  | 0.4 | `test: replace PowerMock with Mockito 5 in core and kit-base unit tests`            | Prerequisite for converting classes the unit tests mock                 |
| [ ]  | 0.5 | `test: expose explicit test seams for package-private core internals`               | Prerequisite for stacks C and D                                         |
| [ ]  | 0.6 | `build(lint): build the custom lint jar from merged Java and Kotlin classes`        | Prerequisite for converting `Logger`                                    |
| [ ]  | 0.7 | `docs: add the Kotlin migration playbook and tracker`                               | This document                                                           |

## Phase 1 · Build hygiene

Separate pull requests, no conversions, invisible to consumers. Only 1.1 must precede the stacks.

| Done | PR   | Title                                                                       | Risk |
| ---- | ---- | --------------------------------------------------------------------------- | ---- |
| [ ]  | 1.1  | `build: declare the Kotlin JVM toolchain explicitly`                        | L    |
| [ ]  | 1.2  | `build: introduce gradle/libs.versions.toml`                                | L    |
| [ ]  | 1.2b | `chore(kits): drop per-kit buildscript pins and sonarqube blocks` (batches) | L    |
| [ ]  | 1.3  | `build: make android-core BuildConfig inputs configuration-cache safe`      | L    |
| [ ]  | 1.4  | `build: modernize deprecated AGP DSL in core and kit-base`                  | L    |
| [ ]  | 1.5  | `build: enable parallel execution and the configuration cache`              | L    |
| [ ]  | 1.6  | `build: remove stale -keep rules from android-core/proguard.pro`            | H    |

Deferred until an Android or Google Play requirement forces them: the lint API version in
`tooling/custom-lint-rules`, the AGP and `compileSdk` refresh, the root Kotlin version, and the
`androidx.annotation` version range in the published POM. Each would change what consumers resolve or
require.

## Phase 2 · Internal conversions

Eight independent stacks. Recommended order: A, D, B, C, G, E, F, H. Every pull request expects a
zero frozen-class diff in `apiCheck` and an empty Binary Compatibility report.

### Stack A · Persistence (2,488 LOC)

| Done | PR  | Files                                                                                                                                                   | LOC | Tests | Risk |
| ---- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | --: | ----- | ---- |
| [ ]  | A1  | `internal/database/tables/*` (7 files), `database/MPDatabase`, `database/UploadSettings`, `services/SQLiteOpenHelperWrapper`, `internal/DatabaseHelper` | 413 | 5/11  | L    |
| [ ]  | A2  | `database/MPDatabaseImpl`, `tables/MParticleDatabaseHelper`, `internal/SegmentDatabase`                                                                 | 368 | 0     | L    |
| [ ]  | A3  | `services/UserAttributesService`, `UploadService`, `BreadcrumbService`, `ReportingService`                                                              | 419 | 4     | L    |
| [ ]  | A4  | `services/SessionService`, `services/MessageService`                                                                                                    | 502 | 2     | L    |
| [ ]  | A5  | `services/MParticleDBManager`                                                                                                                           | 786 | 1     | M    |

### Stack D · Event pipeline (2,726 LOC) · needs 0.4 and 0.5

| Done | PR  | Files                                                                                                                                           |   LOC | Tests | Risk |
| ---- | --- | ----------------------------------------------------------------------------------------------------------------------------------------------- | ----: | ----- | ---- |
| [ ]  | D1  | `internal/MessageManagerCallbacks`; `internal/messages/` MPEventMessage, MPAliasMessage, BaseMPMessage, BaseMPMessageBuilder, MPCommerceMessage |   551 | 1     | L    |
| [ ]  | D2  | `internal/InternalSession` (named in `proguard.pro`; frozen), `internal/BaseHandler`, `internal/MessageBatch`                                   |   392 | 3     | M    |
| [ ]  | D3  | `internal/MessageHandler`                                                                                                                       |   319 | 1     | L    |
| [ ]  | D4  | `internal/UploadHandler`                                                                                                                        |   375 | 1     | M    |
| [ ]  | D5  | `internal/MessageManager`                                                                                                                       | 1,089 | 1     | M    |

### Stack B · Networking (1,270 LOC)

| Done | PR  | Files                                                                                                                     | LOC | Tests | Risk |
| ---- | --- | ------------------------------------------------------------------------------------------------------------------------- | --: | ----- | ---- |
| [ ]  | B1  | `networking/MParticleBaseClient`, `MPUrlImpl`, `MPConnectionImpl`, `NetworkOptionsManager`; `internal/MParticleApiClient` | 319 | 4     | L    |
| [ ]  | B2  | `networking/NetworkConnection`, `networking/MParticleBaseClientImpl` (certificate pinning)                                | 469 | 2     | M    |
| [ ]  | B3  | `internal/MParticleApiClientImpl`                                                                                         | 482 | 1     | M    |

### Stack C · Identity internals (907 LOC) · needs 0.5

| Done | PR  | Files                                                                                                   | LOC | Tests | Risk |
| ---- | --- | ------------------------------------------------------------------------------------------------------- | --: | ----- | ---- |
| [ ]  | C1  | `identity/MParticleIdentityClient`, `identity/MParticleUserImpl`                                        | 170 | 2     | L    |
| [ ]  | C2  | `identity/MParticleUserDelegate`                                                                        | 306 | 2     | L    |
| [ ]  | C3  | `identity/MParticleIdentityClientImpl` (`getStringValue` named in `proguard.pro`; companion keep first) | 431 | 2     | H    |

### Stack G · Platform helpers (1,016 LOC)

| Done | PR  | Files                                                                                               | LOC | Tests | Risk |
| ---- | --- | --------------------------------------------------------------------------------------------------- | --: | ----- | ---- |
| [ ]  | G1  | `ExceptionHandler`, `InstallReferrerHelper`                                                         | 192 | 0     | L    |
| [ ]  | G2  | `internal/PushRegistrationHelper` (named in `proguard.pro`; frozen), `internal/ProviderPersistence` | 263 | 0     | L    |
| [ ]  | G3  | `MPServiceUtil`                                                                                     | 211 | 0     | L    |
| [ ]  | G4  | `internal/DeviceAttributes`                                                                         | 350 | 1     | M    |

### Stack E · Kit bridge, core side (1,002 LOC)

| Done | PR  | Files                                                                                   | LOC | Tests | Risk |
| ---- | --- | --------------------------------------------------------------------------------------- | --: | ----- | ---- |
| [ ]  | E1  | `internal/KitsLoadedListener`, `ReportingManager`, `JsonReportingMessage`, `KitContext` |  54 | 0     | L    |
| [ ]  | E2  | `internal/KitManager` (kit-imported interface)                                          | 139 | 3     | H    |
| [ ]  | E3  | `internal/KitFrameworkWrapper` (reflective loader)                                      | 809 | 2     | H    |

### Stack F · Kit-base internals (1,219 LOC)

| Done | PR  | Files                                              | LOC | Tests | Risk |
| ---- | --- | -------------------------------------------------- | --: | ----- | ---- |
| [ ]  | F1  | `kits/KitIntegrationFactory`                       | 159 | 0     | M    |
| [ ]  | F2  | `kits/mappings/EventWrapper`, `CustomMappingMatch` | 570 | 0     | L    |
| [ ]  | F3  | `kits/mappings/CustomMapping`                      | 490 | 1     | M    |

### Stack H · Frozen utilities (2,572 LOC) · after A–G, needs 0.4 and 0.6

| Done | PR  | Files                    |   LOC | Tests | Risk |
| ---- | --- | ------------------------ | ----: | ----- | ---- |
| [ ]  | H1  | `internal/Logger`        |   200 | 1     | H    |
| [ ]  | H2  | `internal/MPUtility`     |   858 | 2     | H    |
| [ ]  | H3  | `internal/ConfigManager` | 1,514 | 2     | M    |

## Phase 3 · Kit-visible hubs (3,350 LOC) · decided individually at the gate

| Done | PR  | File                            |   LOC | Risk | Fallback                                 |
| ---- | --- | ------------------------------- | ----: | ---- | ---------------------------------------- |
| [ ]  | 3.1 | `kits/KitConfiguration`         | 1,069 | H    | Keep the Java declaration; move the body |
| [ ]  | 3.2 | `kits/KitManagerImpl`           | 1,441 | H    | Keep the Java declaration; move the body |
| [ ]  | 3.3 | `internal/MParticleJSInterface` |   840 | H    | Leave in Java                            |

## Checkpoints

QA points, not releases. Record the result of each in this section when it is reached.

| Point | After                               | Check                                                                                                 |
| ----- | ----------------------------------- | ----------------------------------------------------------------------------------------------------- |
| M0    | Phase 0 and PR 1.1                  | A deliberate removal of a public method on a scratch branch fails `apiCheck` and Binary Compatibility |
| M1    | Stacks A and D                      | Offline queue across process death, sessions, uploads                                                 |
| M2    | Stacks B, C and G                   | Identity flows, remote config, certificate pinning against production, push registration              |
| M3    | Stacks E, F and H; Phase 1 complete | Full checklist; one third-party kit and the Rokt kit end to end on a minified consumer build          |
| Gate  | Before Phase 3                      | Per-file go/no-go with the consumer fixture results                                                   |
| M4    | Phase 3                             | Full checklist; the migration's release                                                               |

## Optional follow-ups

- Move the bodies of the Java facade classes into internal Kotlin helpers while their declarations stay
  put.
- Convert the unpublished `testutils` module (41 files).
- Keep `RoktSessionJavaTest`, `MParticleBaseClientImplTest` (both under `android-core/src/test`) and
  `MParticleJSInterfaceITest` (`android-core/src/androidTest`) in Java: they are the only in-repository
  tests of the Java-caller view.
