# mParticle Android SDK — Java → Kotlin migration

**Status:** planning · **Owner:** Android SDK team · **Started:** Sep 2, 2026 · **Target:** Oct 9, 2026

This is the living tracker. Strike through items and append the PR link as they land,
the same way the Apple SDK's Swift migration doc works.

---

## 1. Goal

Migrate `android-core`, `android-kit-base`, and the Rokt kit to Kotlin **without changing
the public interface**. Every line that is not part of the customer- or kit-author-facing
API surface should be Kotlin by the end of the migration.

This is the Android translation of the principle the Apple SDK is following on
`workstation`: *do not attempt a wholesale rewrite — slice pure subsystems out and leave
the public facade in the original language as the orchestrator.*

For Apple, ObjC keeps the singleton, `sqlite3`, and dynamic dispatch. For Android,
**Java keeps the public API declarations** — the classes and signatures that customers and
external kit authors compile against — and everything behind them becomes Kotlin.

### Baseline (Sep 2, 2026)

| Module | Kotlin LOC | Java LOC | Java that stays (public facade) | Java left to migrate | Kotlin % |
| --- | ---: | ---: | ---: | ---: | ---: |
| `android-core` | 3,225 | 22,457 | 9,241 | **13,216** | 12.6% |
| `android-kit-base` | 557 | 5,380 | 1,651 | **3,729** | 9.4% |
| `kits/rokt/rokt` | 1,122 | 0 | 0 | **0** | 100.0% |
| **Total** | **4,904** | **27,837** | **10,892** | **16,945** | **15.0%** |

**Migration goal progress: 22.4%** — Kotlin as a share of everything that is not a
designated public-API facade. This is the number the CI job reports, and it is the number
that needs to reach 100%.

> The Rokt kit is already 100% Kotlin (8 files, 1,122 LOC in `src/main`). Nothing to do
> there beyond keeping it that way, which the CI ratchet now enforces.

### Out of scope for the short-term migration

- `testutils` (43 Java files) and the remaining 3 Java files in core/kit-base test source
  sets. Worth doing, tracked separately, zero release risk.
- The other 22 kits under `kits/`. Their only Java is sample-app code.
- Any behaviour change, API addition, deprecation, or "while I'm in here" refactor.

---

## 2. The one hard rule: the public interface does not move

A Java → Kotlin conversion is **not** automatically ABI-safe. These are the ways a
mechanical conversion silently breaks customers, and the rule for each.

| Hazard | What breaks | Rule |
| --- | --- | --- |
| `public` field → Kotlin property | Field access becomes `getX()`/`setX()`; source **and** binary break | `@JvmField`, or leave the declaration in Java |
| `public static final` constant | Becomes `Foo.Companion.getBar()` | `const val` (primitives/`String`) or `@JvmField` in a `companion object` |
| `public static` method | Becomes `Foo.Companion.baz()` | `@JvmStatic` on every one |
| Kotlin classes are `final` by default | Anything a customer or kit subclasses stops compiling | Mark `open` — mandatory for `KitIntegration`, `MPReceiver`, `MPService`, `BaseIdentityTask`, every listener |
| Platform types become explicit nullability | A non-null param emits `checkNotNullParameter` — a Java caller that used to pass `null` now throws | Mirror the existing `@NonNull`/`@Nullable` exactly. **Unannotated parameter → `T?`.** Never tighten |
| Default arguments | Generates a different overload set | Do not introduce defaults on public signatures. Keep explicit overloads. Use `@JvmOverloads` only to reproduce an existing set |
| Checked exceptions | `throws` clause disappears from the signature | `@Throws(...)` |
| `synchronized` / `volatile` / `transient` | Modifier is dropped | `@Synchronized` / `@Volatile` / `@Transient` |
| Generic variance | Kotlin inserts `? extends` wildcards | `@JvmSuppressWildcards` where the Java signature had none |
| Getter naming | `val fooEnabled` → `getFooEnabled()`, not `isFooEnabled()` | Match the existing accessor name exactly; `@get:JvmName` if needed |
| `package-info.java` | Has no Kotlin equivalent at all | Stays Java, permanently |

**Two things in our favour:**

- `android-core/consumer-proguard.pro` is `-keep class com.mparticle.** { *; }`, so the
  extra `Foo$Companion` / `Foo$DefaultImpls` classes Kotlin emits will not be stripped or
  renamed in customer apps.
- Java and Kotlin interop within a single Gradle module, so this is a genuine
  file-at-a-time migration. There is no big-bang cutover and no intermediate broken state.

### The facade list

[`scripts/kotlin-migration-facades.txt`](../scripts/kotlin-migration-facades.txt) is the
explicit, reviewed list of Java files that stay Java. A file goes on that list only if it
*declares* public API or is a `package-info.java`. Adding to it is an API-surface decision
and needs an SDK owner's review, not just the PR author's.

Facade files are still expected to **shrink** — their bodies move into internal Kotlin and
the Java file thins out to declarations that delegate. They are just excluded from the
"Java left to migrate" number so that number can honestly reach zero.

---

## 3. How to do a conversion

1. **One file per PR.** Stack them with Graphite inside your lane. Do not touch a file
   that belongs to another lane; if you need a change there, ask the lane owner to stack
   it, or note it in the Cleanup section.
2. **Characterization test first.** If the file has no meaningful test coverage, the PR
   *below* it in the stack adds tests against the current Java behaviour. Converting an
   untested file is how a silent behaviour change ships.
3. **Convert, do not refactor.** Use IntelliJ's *Convert Java File to Kotlin File*, then
   clean up only what the compiler and ktlint demand. Renames, redesigns, coroutine
   adoption, and null-safety improvements are all separate follow-up PRs.
4. **Land the file in `src/main/kotlin/`,** matching the package path. (We currently have
   5 stragglers under `src/main/java/` — see Cleanup.)
5. **Armour the JVM signature** per the table above, even for internal classes: `kits/`
   and `testutils` compile against a lot of `com.mparticle.internal`.
6. **Run the module's tests** — `./gradlew :android-core:test :android-core:cAT` — plus
   `./gradlew ktlintCheck`. For anything in `android-kit-base` or `internal/Kit*`, also
   run the kit compatibility job locally.
7. **Check the CI comment.** Every PR gets a Kotlin-migration comment showing the LOC it
   moved and the running total.

### Subsystem-specific guidance

- **Kit container and kit ABI.** `KitFrameworkWrapper` reflectively loads
  `com.mparticle.kits.KitManagerImpl` by name via a 4-arg constructor, and 50 external kit
  files subclass `KitIntegration`. Keep the class name, the constructor arity, and the
  `open`-ness. `KitIntegration`, `KitUtils`, `ReportingMessage`, `CommerceEventUtils`,
  `FilteredMParticleUser`, and `FilteredIdentityApiRequest` stay Java — they are the kit
  ABI. `KitConfiguration`, `KitManagerImpl`, `KitIntegrationFactory`, and everything under
  `mappings/` are ours and can move.
- **Persistence.** Unlike Apple's `sqlite3` C-API problem, Kotlin owns
  `SQLiteDatabase`/`Cursor` as cleanly as Java does. The risk here is schema versions and
  queued-event behaviour, not language interop. Convert tables → services → `MParticleDBManager`
  in that order, and pair every slice with the existing DB tests.
- **`MParticleJSInterface`.** Every `@JavascriptInterface` method must keep its exact name
  and JVM signature — the WebView bridge resolves them by name at runtime, and JS callers
  are outside our repo. Treat this as a public interface even though it lives in
  `internal`. Convert last, with the JS bridge instrumented tests green.
- **`MParticle` / `MParticleOptions` / `ConfigManager`.** These stay large Java facades
  the longest. "Converting" them means pulling session/opt-out/config policy into internal
  Kotlin while the singleton, `start()`, `getInstance()`, the nested enums
  (`EventType`, `IdentityType`, `Environment`, …), `ServiceProviders`, and the builder
  chain keep their current Java declarations and signatures.
- **Reflection sites.** `MPUtility`, `PushRegistrationHelper`, `MPReceiver`, and
  `MPServiceUtil` all do `Class.forName` on *third-party* classes (Play Services, Firebase,
  Install Referrer). Those strings are external contracts — copy them verbatim, do not
  "clean them up".

---

## 4. Work breakdown

Eight lanes, split by directory so that two people never edit the same file. Stack PRs
within a lane; lanes are independent and can run fully in parallel.

Assumes 3–4 engineers; one person can own two adjacent lanes. **Lane H is single-owner and
goes last** — it is the cross-cutting code everything else touches.

### Lane A — Persistence · 2,488 LOC · owner: _unassigned_

*Easy*
- [ ] `internal/database/tables/MpIdDependentTable.java` — 17
- [ ] `internal/database/tables/UserAttributesTable.java` — 30
- [ ] `internal/database/tables/BreadcrumbTable.java` — 32
- [ ] `internal/database/tables/ReportingTable.java` — 35
- [ ] `internal/database/tables/UploadTable.java` — 38
- [ ] `internal/database/tables/MessageTable.java` — 46
- [ ] `internal/database/tables/SessionTable.java` — 60
- [ ] `internal/database/services/SQLiteOpenHelperWrapper.java` — 12
- [ ] `internal/database/MPDatabase.java` — 29
- [ ] `internal/DatabaseHelper.java` — 38
- [ ] `internal/SegmentDatabase.java` — 66
- [ ] `internal/database/UploadSettings.java` — 76
- [ ] `internal/database/MPDatabaseImpl.java` — 127
- [ ] `internal/database/tables/MParticleDatabaseHelper.java` — 175 *(schema versions — read carefully)*

*Medium*
- [ ] `internal/database/services/UserAttributesService.java` — 86
- [ ] `internal/database/services/UploadService.java` — 104
- [ ] `internal/database/services/BreadcrumbService.java` — 112
- [ ] `internal/database/services/ReportingService.java` — 117
- [ ] `internal/database/services/SessionService.java` — 205
- [ ] `internal/database/services/MessageService.java` — 297

*Hard*
- [ ] `internal/database/services/MParticleDBManager.java` — 786 — split into ≥3 stacked PRs (message/session ops, user-attribute ops, upload+alias ops)

### Lane B — Networking · 1,270 LOC · owner: _unassigned_

*Easy*
- [ ] `networking/MParticleBaseClient.java` — 7
- [ ] `networking/MPUrlImpl.java` — 51
- [ ] `networking/NetworkOptionsManager.java` — 91
- [ ] `networking/MPConnectionImpl.java` — 113
- [ ] `internal/MParticleApiClient.java` — 57

*Medium*
- [ ] `networking/NetworkConnection.java` — 147
- [ ] `networking/MParticleBaseClientImpl.java` — 322 *(URL construction, cert pinning — see the Sept 2026 pinning incident)*
- [ ] `internal/MParticleApiClientImpl.java` — 482 — split: request building / response parsing / retry+throttle

`NetworkOptions`, `DomainMapping`, `Certificate`, `MPUrl`, `MPConnection`,
`BaseNetworkConnection` are public and stay Java.

### Lane C — Identity internals · 907 LOC · owner: _unassigned_

*Easy*
- [ ] `identity/MParticleIdentityClient.java` — 13
- [ ] `identity/MParticleUserImpl.java` — 157

*Medium*
- [ ] `identity/MParticleUserDelegate.java` — 306
- [ ] `identity/MParticleIdentityClientImpl.java` — 431 — split: request serialization / response handling

`IdentityApi`, `MParticleUser`, `IdentityApiRequest`, `AliasRequest`, and the
task/listener types are public and stay Java.

### Lane D — Event pipeline · 2,726 LOC · owner: _unassigned_

*Easy*
- [ ] `internal/MessageManagerCallbacks.java` — 26
- [ ] `internal/messages/MPEventMessage.java` — 39
- [ ] `internal/messages/MPAliasMessage.java` — 77
- [ ] `internal/messages/BaseMPMessage.java` — 92
- [ ] `internal/InternalSession.java` — 95
- [ ] `internal/BaseHandler.java` — 99
- [ ] `internal/messages/BaseMPMessageBuilder.java` — 144
- [ ] `internal/messages/MPCommerceMessage.java` — 173
- [ ] `internal/MessageBatch.java` — 198

*Medium*
- [ ] `internal/MessageHandler.java` — 319
- [ ] `internal/UploadHandler.java` — 375

*Hard*
- [ ] `internal/MessageManager.java` — 1,089 — split into ≥4 stacked PRs (message construction, session lifecycle, attribute messages, the handler/queue orchestrator last)

### Lane E — Kit infrastructure, core side · 1,002 LOC · owner: _unassigned_

*Easy*
- [ ] `internal/KitsLoadedListener.java` — 6
- [ ] `internal/ReportingManager.java` — 10
- [ ] `internal/JsonReportingMessage.java` — 17
- [ ] `internal/KitContext.java` — 21
- [ ] `internal/KitManager.java` — 139 *(interface — referenced by 4 kit files)*

*Hard*
- [ ] `internal/KitFrameworkWrapper.java` — 809 — split: kit-availability checks / queued-call replay / the reflective `KitManagerImpl` loader last. **The `Class.forName("com.mparticle.kits.KitManagerImpl")` call and its 4-arg constructor must not change.**

### Lane F — Kit base internals · 3,729 LOC · owner: _unassigned_

*Medium*
- [ ] `kits/KitIntegrationFactory.java` — 159
- [ ] `kits/mappings/EventWrapper.java` — 279
- [ ] `kits/mappings/CustomMappingMatch.java` — 291
- [ ] `kits/mappings/CustomMapping.java` — 490 — split: match evaluation / projection application

*Hard*
- [ ] `kits/KitConfiguration.java` — 1,069 — split: filter maps parsing / bracketing+sampling / config accessors. Referenced by 29 kit files — audit every public member before touching it
- [ ] `kits/KitManagerImpl.java` — 1,441 — split into ≥4 stacked PRs (kit lifecycle, event forwarding, commerce forwarding, identity/user forwarding). Referenced by 12 kit files; class name and constructor are load-bearing

### Lane G — Platform surface · 1,411 LOC · owner: _unassigned_

*Easy*
- [ ] `messaging/MessagingConfigCallbacks.java` — 7
- [ ] `messaging/PushAnalyticsReceiverCallback.java` — 19
- [ ] `messaging/InstanceIdService.java` — 20
- [ ] `messaging/MPMessagingRouter.java` — 38
- [ ] `messaging/PushAnalyticsReceiver.java` — 41
- [ ] `ExceptionHandler.java` — 50

*Medium*
- [ ] `internal/PushRegistrationHelper.java` — 114 *(Firebase reflection — copy strings verbatim)*
- [ ] `InstallReferrerHelper.java` — 142
- [ ] `internal/ProviderPersistence.java` — 149
- [ ] `MPServiceUtil.java` — 211
- [ ] `messaging/ProviderCloudMessage.java` — 270
- [ ] `internal/DeviceAttributes.java` — 350

`MPService`, `MPReceiver`, `MPMessagingAPI`, `MPMediaAPI`, and `segmentation/*` are public
and stay Java.

### Lane H — Cross-cutting internals · 3,412 LOC · single owner, goes last

- [ ] `internal/Logger.java` — 200 *(referenced by 32 kit files — `@JvmStatic` everywhere)*
- [ ] `internal/MPUtility.java` — 858 — split by concern (JSON helpers, device/ID helpers, reflection probes, hashing). Referenced by 14 kit files
- [ ] `internal/ConfigManager.java` — 1,514 — split into ≥5 stacked PRs (prefs accessors, kit-config parsing, data-plan config, listener registration, the orchestrator last)
- [ ] `internal/MParticleJSInterface.java` — 840 — **last file in the migration.** Every `@JavascriptInterface` name and signature is frozen

### Lane I — Facade thinning (after H)

Once internal Java is at zero, the remaining Java is the facade. Move the *bodies* into
internal Kotlin while the Java declarations stay put. Highest value first:

- [ ] `MParticle.java` — 1,958 → pull opt-out checks, session policy, and attribute
      plumbing into Kotlin; keep the singleton, statics, nested enums and `ServiceProviders`
- [ ] `MParticleOptions.java` — 1,108 → move builder validation and defaulting into Kotlin;
      keep the builder chain and every setter signature
- [ ] `commerce/CommerceEvent.java` — 873 + `Product.java` — 528 → move validation/serialization
- [ ] `MPEvent.java` — 591 → move attribute normalization
- [ ] `identity/IdentityApi.java` — 482 → move request assembly and callback dispatch

### Cleanup — opportunistic wins, add as you find them

- [ ] Move 5 stray `.kt` files out of `src/main/java/` into `src/main/kotlin/`:
      `internal/KitsLoadedCallback.kt`, `internal/UserAudiencesRetriever.kt`,
      `internal/SideloadedKitsUtils.kt`, `internal/KitsLoadedListenerConfiguration.kt`,
      `kits/MPSideloadedKit.kt` + `kits/MPSideloadedFilters.kt`
- [ ] `testutils` — 43 Java files, no release risk
- [ ] Remaining 3 Java files in core/kit-base test source sets

---

## 5. CI: progress tracking and guard rails

### Progress tracker (landed)

- [`scripts/kotlin-migration-progress.sh`](../scripts/kotlin-migration-progress.sh)
  measures Kotlin vs Java LOC across the three in-scope `src/main` source sets, excluding
  `build/`, tests, and sample apps.
- The `kotlin-migration-progress` job in
  [`.github/workflows/pull-request.yml`](../.github/workflows/pull-request.yml) runs it on
  the PR head *and* on the base commit, then posts a single sticky PR comment with:
  the lines this PR moved, the per-module table, the overall goal percentage, and the 15
  largest Java files still outstanding. The same content goes to the job summary.
- The base measurement always uses the PR's *current* facade list, so adding or removing a
  facade entry can never masquerade as migration progress.

### Ratchet (landed)

The job fails if a PR increases Java LOC in the three in-scope modules. New code in
`android-core`, `android-kit-base`, and the Rokt kit is Kotlin from now on. The escape
hatch is the `allow-new-java` label plus a reason in the PR description.

> **Setup needed:** create the `allow-new-java` label in the repo. The job requests
> `pull-requests: write` at the job level; the workflow default stays `read`.

### API guard (M0 — not yet built)

The tracker measures progress; it does not prove we kept the promise. We need a check that
fails when a public signature changes.

- [ ] **Spike (½ day):** try
      `org.jetbrains.kotlinx.binary-compatibility-validator` on `:android-core` and
      `:android-kit-base`. If it cooperates with AGP 8.3, commit `api/*.api` dumps and wire
      `apiCheck` into the PR workflow.
- [ ] **Fallback if BCV fights AGP:** a ~60-line Gradle task that runs `javap -public` over
      the release variant's classes, writes a sorted `api/android-core.api` text file, and
      fails on diff. Zero new plugin dependencies, same effect, and it reviews well —
      the diff *is* the API change, visible in the PR.
- [ ] Either way: a changed `.api` file requires an SDK owner's approval, exactly like the
      facade list.

---

## 6. Milestones

> Assumes 3–4 engineers with lanes running in parallel. Dates are Fridays.

### M0 — Sep 4 · Foundations
- [x] Progress tracker script + CI job + ratchet
- [x] Facade list agreed and committed
- [ ] `allow-new-java` label created
- [ ] API guard spike done, `apiCheck` (or the `javap` fallback) wired into CI
- [ ] Lane owners assigned
- [ ] Stray `.kt` files moved to `src/main/kotlin`

### M1 — Sep 11 · Easy tier · 2,581 LOC · target ≈ 34% goal progress
- [ ] Lane A easy (14 files, 781 LOC)
- [ ] Lane B easy (5 files, 319 LOC)
- [ ] Lane D easy (9 files, 943 LOC)
- [ ] Lane E easy (5 files, 193 LOC)
- [ ] Lane G easy (6 files, 175 LOC)
- [ ] Lane C easy (2 files, 170 LOC)

### M2 — Sep 18 · Medium tier · 5,758 LOC · target ≈ 61%
- [ ] Lane A services (6 files, 921 LOC)
- [ ] Lane B medium (3 files, 951 LOC)
- [ ] Lane C medium (2 files, 737 LOC)
- [ ] Lane D medium (2 files, 694 LOC)
- [ ] Lane G medium (6 files, 1,236 LOC)
- [ ] Lane F medium (4 files, 1,219 LOC)

### M3 — Sep 25 · Hard tier, part 1 · 3,753 LOC · target ≈ 78%
- [ ] `MParticleDBManager` (786)
- [ ] `MessageManager` (1,089)
- [ ] `KitFrameworkWrapper` (809)
- [ ] `KitConfiguration` (1,069)
- [ ] **Manual QA pass 1** — event stream, identity, kit forwarding, Rokt kit

### M4 — Oct 2 · Hard tier, part 2 · 4,853 LOC · target 100% of internal Java
- [ ] `KitManagerImpl` (1,441)
- [ ] `Logger` (200), `MPUtility` (858)
- [ ] `ConfigManager` (1,514)
- [ ] `MParticleJSInterface` (840)
- [ ] **Internal Java LOC = 0** — the tracker reads 100%

### M5 — Oct 9 · Facade thinning, QA, release
- [ ] Lane I: `MParticle`, `MParticleOptions`, `CommerceEvent`/`Product`, `MPEvent`, `IdentityApi` bodies moved to Kotlin
- [ ] `apiCheck` diff across the whole migration reviewed and confirmed empty
- [ ] **Manual QA pass 2** (full)
- [ ] Cleanup items
- [ ] Release

---

## 7. Manual QA checklist

Run at M3 and M5, against a real workspace, on a minSdk and a current-SDK device.

- [ ] Event stream — custom events, screen views, commerce events reach the dashboard with
      correct attributes
- [ ] Session handling — start/end/timeout, background/foreground transitions
- [ ] Config — remote config fetch, kit config application, data-plan filtering
- [ ] Persistence — events queued offline, uploaded on reconnect, no loss across process death
- [ ] Identity — login, logout, identify, modify, alias
- [ ] Consent — GDPR and CCPA state round-trips
- [ ] Push — registration, receipt, analytics
- [ ] Kits — at least Braze, GA4, UrbanAirship forwarding correctly
- [ ] **Rokt kit** — placement render, callbacks, `RoktEmbeddedView`
- [ ] WebView bridge — `MParticleJSInterface` end to end
- [ ] R8/ProGuard release build of the sample app — no missing-class warnings

---

## 8. Risks

| Risk | Mitigation |
| --- | --- |
| A conversion silently tightens nullability and a customer's `null` starts throwing | The unannotated-param → `T?` rule; API guard diff; characterization tests |
| A Kotlin class becomes `final` and breaks a kit author | Facade list keeps the kit ABI in Java; kit-compatibility CI job runs on every PR |
| `MParticleJSInterface` method renamed by the converter | Converted last, JS bridge instrumented tests must pass, names frozen |
| `KitManagerImpl` reflective load breaks | Class name and constructor arity are explicitly frozen; kit-compatibility job covers it |
| Merge conflicts across a 17k-LOC migration | Directory-scoped lanes, one file per PR, Graphite stacks within a lane |
| Migration drags past the window | Ratchet stops the problem growing; the facade split means we can stop at any milestone and still be in a shippable, non-breaking state |
