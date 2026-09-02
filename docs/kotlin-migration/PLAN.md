# mParticle Android SDK — Java → Kotlin migration

**Status:** planning · **Owner:** Android SDK team · **Milestones:** percentage gates, not dates (§6)

This is the living tracker. Strike through items and append the PR link as they land,
the same way the Apple SDK's Swift migration doc works.

---

## 1. Goal

Migrate `android-core`, `android-kit-base`, and the Rokt kit to Kotlin **without changing
the public interface**. Every line that is not part of the customer- or kit-author-facing
API surface should be Kotlin by the end of the migration.

This is the Android translation of the principle the Apple SDK is following on
`workstation`: _do not attempt a wholesale rewrite — slice pure subsystems out and leave
the public facade in the original language as the orchestrator._

For Apple, ObjC keeps the singleton, `sqlite3`, and dynamic dispatch. For Android,
**Java keeps the public API declarations** — the classes and signatures that customers and
external kit authors compile against — and everything behind them becomes Kotlin.

### Baseline (measured at `13216621`)

| Module             | Kotlin LOC |   Java LOC | Java that stays (public facade) | Java left to migrate |  Kotlin % |
| ------------------ | ---------: | ---------: | ------------------------------: | -------------------: | --------: |
| `android-core`     |      3,225 |     22,457 |                           9,241 |           **13,216** |     12.6% |
| `android-kit-base` |        557 |      5,380 |                           1,651 |            **3,729** |      9.4% |
| `kits/rokt/rokt`   |      1,122 |          0 |                               0 |                **0** |    100.0% |
| **Total**          |  **4,904** | **27,837** |                      **10,892** |           **16,945** | **15.0%** |

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

| Hazard                                     | What breaks                                                                                        | Rule                                                                                                                           |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `public` field → Kotlin property           | Field access becomes `getX()`/`setX()`; source **and** binary break                                | `@JvmField`, or leave the declaration in Java                                                                                  |
| `public static final` constant             | Becomes `Foo.Companion.getBar()`                                                                   | `const val` (primitives/`String`) or `@JvmField` in a `companion object`                                                       |
| `public static` method                     | Becomes `Foo.Companion.baz()`                                                                      | `@JvmStatic` on every one                                                                                                      |
| Kotlin classes are `final` by default      | Anything a customer or kit subclasses stops compiling                                              | Mark `open` — mandatory for `KitIntegration`, `MPReceiver`, `MPService`, `BaseIdentityTask`, every listener                    |
| Platform types become explicit nullability | A non-null param emits `checkNotNullParameter` — a Java caller that used to pass `null` now throws | Mirror the existing `@NonNull`/`@Nullable` exactly. **Unannotated parameter → `T?`.** Never tighten                            |
| Default arguments                          | Generates a different overload set                                                                 | Do not introduce defaults on public signatures. Keep explicit overloads. Use `@JvmOverloads` only to reproduce an existing set |
| Checked exceptions                         | `throws` clause disappears from the signature                                                      | `@Throws(...)`                                                                                                                 |
| `synchronized` / `volatile` / `transient`  | Modifier is dropped                                                                                | `@Synchronized` / `@Volatile` / `@Transient`                                                                                   |
| Generic variance                           | Kotlin inserts `? extends` wildcards                                                               | `@JvmSuppressWildcards` where the Java signature had none                                                                      |
| Getter naming                              | `val fooEnabled` → `getFooEnabled()`, not `isFooEnabled()`                                         | Match the existing accessor name exactly; `@get:JvmName` if needed                                                             |
| `package-info.java`                        | Has no Kotlin equivalent at all                                                                    | Stays Java, permanently                                                                                                        |

**Two things in our favour:**

- `android-core/consumer-proguard.pro` is `-keep class com.mparticle.** { *; }`, so the
  extra `Foo$Companion` / `Foo$DefaultImpls` classes Kotlin emits will not be stripped or
  renamed in customer apps.
- Java and Kotlin interop within a single Gradle module, so this is a genuine
  file-at-a-time migration. There is no big-bang cutover and no intermediate broken state.

### The facade list

[`scripts/kotlin-migration-facades.txt`](../../scripts/kotlin-migration-facades.txt) is the
explicit, reviewed list of Java files that stay Java. A file goes on that list only if it
_declares_ public API or is a `package-info.java`. Adding to it is an API-surface decision
and needs an SDK owner's review, not just the PR author's.

Facade files are still expected to **shrink** — their bodies move into internal Kotlin and
the Java file thins out to declarations that delegate. They are just excluded from the
"Java left to migrate" number so that number can honestly reach zero.

---

## 3. How to do a conversion

1. **Batch the easy tier, one file per PR for everything else.** Easy-tier files are
   leaf classes — table schemas, message types, small interfaces — and reviewing seven of
   them together is no harder than reviewing one. Batch them per lane directory: roughly
   8 PRs instead of 41. Medium and hard files stay one per PR, stacked with Graphite
   inside your lane. Never touch a file that belongs to another lane; ask that owner to
   stack it, or note it in Cleanup.

    This is the single biggest lever on how fast the percentage moves. It is only safe
    because the API guard reports exactly which signatures moved and the ratchet
    blocks new Java — without both, batch conversions are how a silent break ships.

2. **Characterization test first.** If the file has no meaningful test coverage, the PR
   _below_ it in the stack adds tests against the current Java behaviour. Converting an
   untested file is how a silent behaviour change ships.
3. **Convert, do not refactor.** Use IntelliJ's _Convert Java File to Kotlin File_, then
   clean up only what the compiler and ktlint demand. Renames, redesigns, coroutine
   adoption, and null-safety improvements are all separate follow-up PRs.
4. **Convert in place.** Leave the file in the directory it already lives in and change
   the extension. Both `src/main/java` and `src/main/kotlin` are configured as Kotlin
   source directories in every module and Kotlin already compiles from both today, so
   there is no separation to maintain and nothing to move. A conversion should be a
   content diff, not a rename plus a content diff — and a rename is exactly the kind of
   noise that hides a signature change in review.
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
  `MPServiceUtil` all do `Class.forName` on _third-party_ classes (Play Services, Firebase,
  Install Referrer). Those strings are external contracts — copy them verbatim, do not
  "clean them up".

---

## 4. Work breakdown

Eight lanes, split by directory so that two people never edit the same file. Stack PRs
within a lane; lanes are independent and run fully in parallel.

**Three engineers, three groups of lanes.** Lanes are grouped so each owner holds a
coherent subsystem rather than a scattering of files:

| Owner | Lanes                                                   |   LOC |
| ----- | ------------------------------------------------------- | ----: |
| 1     | F + E + C — the kit container, both sides, and identity | 5,638 |
| 2     | H + A — cross-cutting internals and persistence         | 5,900 |
| 3     | D + G + B — the runtime data path                       | 5,407 |

Nobody owns a lane that another owner's lane edits, so the three streams never collide.
If a fourth pair of hands appears, the cleanest split is to lift H out of owner 2's group
and give it to them whole.

**Lane H runs in parallel rather than last.** Holding 3,412 lines back until everything
else is done was conservatism, and with three engineers it would idle one of them. It is
safe because
lanes are file-scoped and Java and Kotlin interoperate inside one module, so nobody is
blocked on H converting. The residual risk is a semantic conflict between a caller in
another lane and an H callee converted the same day — tests catch that, the API guard
does not. Lane H's owner posts in the team channel when each file merges.

### Lane A — Persistence · 2,488 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

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
- [ ] `internal/database/tables/MParticleDatabaseHelper.java` — 175 _(schema versions — read carefully)_

**Medium** — split the larger files into stacked PRs.

- [ ] `internal/database/services/UserAttributesService.java` — 86
- [ ] `internal/database/services/UploadService.java` — 104
- [ ] `internal/database/services/BreadcrumbService.java` — 112
- [ ] `internal/database/services/ReportingService.java` — 117
- [ ] `internal/database/services/SessionService.java` — 205
- [ ] `internal/database/services/MessageService.java` — 297

**Hard** — split by concern, orchestrator last.

- [ ] `internal/database/services/MParticleDBManager.java` — 786 — split into ≥3 stacked PRs (message/session ops, user-attribute ops, upload+alias ops)

### Lane B — Networking · 1,270 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

- [ ] `networking/MParticleBaseClient.java` — 7
- [ ] `networking/MPUrlImpl.java` — 51
- [ ] `networking/NetworkOptionsManager.java` — 91
- [ ] `networking/MPConnectionImpl.java` — 113
- [ ] `internal/MParticleApiClient.java` — 57

**Medium** — split the larger files into stacked PRs.

- [ ] `networking/NetworkConnection.java` — 147
- [ ] `networking/MParticleBaseClientImpl.java` — 322 _(URL construction, cert pinning — see the cert-pinning incident)_
- [ ] `internal/MParticleApiClientImpl.java` — 482 — split: request building / response parsing / retry+throttle

`NetworkOptions`, `DomainMapping`, `Certificate`, `MPUrl`, `MPConnection`,
`BaseNetworkConnection` are public and stay Java.

### Lane C — Identity internals · 907 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

- [ ] `identity/MParticleIdentityClient.java` — 13
- [ ] `identity/MParticleUserImpl.java` — 157

**Medium** — split the larger files into stacked PRs.

- [ ] `identity/MParticleUserDelegate.java` — 306
- [ ] `identity/MParticleIdentityClientImpl.java` — 431 — split: request serialization / response handling

`IdentityApi`, `MParticleUser`, `IdentityApiRequest`, `AliasRequest`, and the
task/listener types are public and stay Java.

### Lane D — Event pipeline · 2,726 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

- [ ] `internal/MessageManagerCallbacks.java` — 26
- [ ] `internal/messages/MPEventMessage.java` — 39
- [ ] `internal/messages/MPAliasMessage.java` — 77
- [ ] `internal/messages/BaseMPMessage.java` — 92
- [ ] `internal/InternalSession.java` — 95
- [ ] `internal/BaseHandler.java` — 99
- [ ] `internal/messages/BaseMPMessageBuilder.java` — 144
- [ ] `internal/messages/MPCommerceMessage.java` — 173
- [ ] `internal/MessageBatch.java` — 198

**Medium** — split the larger files into stacked PRs.

- [ ] `internal/MessageHandler.java` — 319
- [ ] `internal/UploadHandler.java` — 375

**Hard** — split by concern, orchestrator last.

- [ ] `internal/MessageManager.java` — 1,089 — split into ≥4 stacked PRs (message construction, session lifecycle, attribute messages, the handler/queue orchestrator last)

### Lane E — Kit infrastructure, core side · 1,002 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

- [ ] `internal/KitsLoadedListener.java` — 6
- [ ] `internal/ReportingManager.java` — 10
- [ ] `internal/JsonReportingMessage.java` — 17
- [ ] `internal/KitContext.java` — 21
- [ ] `internal/KitManager.java` — 139 _(interface — referenced by 4 kit files)_

**Hard** — split by concern, orchestrator last.

- [ ] `internal/KitFrameworkWrapper.java` — 809 — split: kit-availability checks / queued-call replay / the reflective `KitManagerImpl` loader last. **The `Class.forName("com.mparticle.kits.KitManagerImpl")` call and its 4-arg constructor must not change.**

### Lane F — Kit base internals · 3,729 LOC · owner: _unassigned_

**Medium** — split the larger files into stacked PRs.

- [ ] `kits/KitIntegrationFactory.java` — 159
- [ ] `kits/mappings/EventWrapper.java` — 279
- [ ] `kits/mappings/CustomMappingMatch.java` — 291
- [ ] `kits/mappings/CustomMapping.java` — 490 — split: match evaluation / projection application

**Hard** — split by concern, orchestrator last.

- [ ] `kits/KitConfiguration.java` — 1,069 — split: filter maps parsing / bracketing+sampling / config accessors. Referenced by 29 kit files — audit every public member before touching it
- [ ] `kits/KitManagerImpl.java` — 1,441 — split into ≥4 stacked PRs (kit lifecycle, event forwarding, commerce forwarding, identity/user forwarding). Referenced by 12 kit files; class name and constructor are load-bearing

### Lane G — Platform surface · 1,411 LOC · owner: _unassigned_

**Easy** — leaf classes; convert straight across.

- [ ] `messaging/MessagingConfigCallbacks.java` — 7
- [ ] `messaging/PushAnalyticsReceiverCallback.java` — 19
- [ ] `messaging/InstanceIdService.java` — 20
- [ ] `messaging/MPMessagingRouter.java` — 38
- [ ] `messaging/PushAnalyticsReceiver.java` — 41
- [ ] `ExceptionHandler.java` — 50

**Medium** — split the larger files into stacked PRs.

- [ ] `internal/PushRegistrationHelper.java` — 114 _(Firebase reflection — copy strings verbatim)_
- [ ] `InstallReferrerHelper.java` — 142
- [ ] `internal/ProviderPersistence.java` — 149
- [ ] `MPServiceUtil.java` — 211
- [ ] `messaging/ProviderCloudMessage.java` — 270
- [ ] `internal/DeviceAttributes.java` — 350

`MPService`, `MPReceiver`, `MPMessagingAPI`, `MPMediaAPI`, and `segmentation/*` are public
and stay Java.

### Lane H — Cross-cutting internals · 3,412 LOC · single owner, goes last

- [ ] `internal/Logger.java` — 200 _(referenced by 32 kit files — `@JvmStatic` everywhere)_
- [ ] `internal/MPUtility.java` — 858 — split by concern (JSON helpers, device/ID helpers, reflection probes, hashing). Referenced by 14 kit files
- [ ] `internal/ConfigManager.java` — 1,514 — split into ≥5 stacked PRs (prefs accessors, kit-config parsing, data-plan config, listener registration, the orchestrator last)
- [ ] `internal/MParticleJSInterface.java` — 840 — **last file in the migration.** Every `@JavascriptInterface` name and signature is frozen

### Lane I — Facade thinning (deferred; not part of the 100% gate)

Once internal Java is at zero, the remaining Java is the facade. Move the _bodies_ into
internal Kotlin while the Java declarations stay put. Highest value first:

- [ ] `MParticle.java` — 1,958 → pull opt-out checks, session policy, and attribute
      plumbing into Kotlin; keep the singleton, statics, nested enums and `ServiceProviders`
- [ ] `MParticleOptions.java` — 1,108 → move builder validation and defaulting into Kotlin;
      keep the builder chain and every setter signature
- [ ] `commerce/CommerceEvent.java` — 873 + `Product.java` — 528 → move validation/serialization
- [ ] `MPEvent.java` — 591 → move attribute normalization
- [ ] `identity/IdentityApi.java` — 482 → move request assembly and callback dispatch

### Cleanup — opportunistic wins, add as you find them

- [ ] `testutils` — 43 Java files, no release risk
- [ ] Remaining 3 Java files in core/kit-base test source sets

---

## 5. CI: progress tracking and guard rails

### Progress tracker (landed)

- [`scripts/kotlin-migration-progress.sh`](../../scripts/kotlin-migration-progress.sh)
  measures Kotlin vs Java LOC across the three in-scope `src/main` source sets, excluding
  `build/`, tests, and sample apps.
- The `kotlin-migration-progress` job in
  [`.github/workflows/pull-request.yml`](../../.github/workflows/pull-request.yml) runs it on
  the PR head _and_ on the base commit, then posts a single sticky PR comment with:
  the lines this PR moved, the per-module table, the overall goal percentage, and the 15
  largest Java files still outstanding. The same content goes to the job summary.
- The base measurement always uses the PR's _current_ facade list, so adding or removing a
  facade entry can never masquerade as migration progress.

### Ratchet (landed)

The job fails if a PR increases Java LOC in the three in-scope modules. New code in
`android-core`, `android-kit-base`, and the Rokt kit is Kotlin from now on. The escape
hatch is the `allow-new-java` label plus a reason in the PR description.

> **Setup needed:** create the `allow-new-java` label in the repo. The job requests
> `pull-requests: write` at the job level; the workflow default stays `read`.

### API guard (landed)

The tracker measures progress; it does not prove we kept the promise.
[`scripts/api-guard.sh`](../../scripts/api-guard.sh) does.

- It compiles both modules to their AGP compile jars — the exact jars a consumer compiles
  against — and diffs every public class, method and field signature against committed
  baselines in `android-core/api/` and `android-kit-base/api/`. 3,075 declarations for core,
  453 for kit-base at the baseline. It reads signatures with `javap`; it never runs SDK code.
- Compiled signatures are the right surface because Kotlin breaks a Java contract in ways
  that are invisible in source: a property replacing a public field, `Companion` indirection
  on statics, a `DefaultConstructorMarker` overload from a default argument, a `final` class,
  a synthetic public lambda class.
- A diff outside `com.mparticle.internal`, or touching one of the internal classes listed in
  [`scripts/api-guard-frozen-internals.txt`](../../scripts/api-guard-frozen-internals.txt),
  exits **2** and is a gate failure. A diff confined to other internals exits **1**:
  regenerate the baseline in the same PR with the diff and a kit-usage audit in the
  description.
- [`scripts/api-guard-selftest.sh`](../../scripts/api-guard-selftest.sh) proves the guard
  catches breakage, by mutating a copy of the compile jar in a temp directory and asserting
  the clean tree passes, a deleted public class fails as frozen, and a deleted internal-only
  class is reported as reviewable.
- The `public-api-guard` job runs the self-test and the check on every PR.

Policy and the full working agreement: [`PR-GATE.md`](PR-GATE.md).

---

## 6. Milestones

**There are no dates here, on purpose.** With three engineers and priorities that move, a
calendar turns a healthy project into a failed one the moment something else takes
precedence — the work is fine, the plan just says otherwise. Every milestone below is a
gate on the one number CI already prints on each PR, so the migration can be put down and
picked back up at whatever percentage it was left at.

That number is **migration goal progress**: Kotlin as a share of everything that is not a
designated public-API facade. `scripts/kotlin-migration-progress.sh` computes it and the
sticky PR comment carries it. Denominator: 21,849 lines.

Order matters more than pace. Each gate is reached by finishing a tier across every lane,
because a tier finished everywhere is a coherent state to stop at; three lanes at 90% and
one at 10% is not.

### M0 — Tooling · gate: tracker reads 22.4%, CI green

- [x] Progress tracker, CI job and ratchet
- [x] Facade list agreed and committed
- [x] API guard, self-test and baselines; `public-api-guard` job wired into CI
- [ ] `allow-new-java` label created
- [ ] Three lane owners assigned (§4)
- [ ] Batching rule agreed — easy tier per directory, one file per PR after that
- [ ] **Every owner has one conversion PR merged.** Find the friction in review and CI on
      the first PR, not the fortieth.

### M1 — gate: 34% · Easy tier, every lane

2,581 LOC across 41 leaf files, batched into ~8 PRs.
Lane A (781) · Lane D (943) · Lane B (319) · Lane E (193) · Lane G (175) · Lane C (170).

### M2 — gate: 61% · Medium tier, every lane

5,758 LOC. Lane G (1,236) · Lane F mappings (1,219) · Lane B (951) · Lane A services
(921) · Lane C (737) · Lane D handlers (694).

- [ ] **Manual QA pass 1 — full checklist (§7).** The medium tier is where the behaviour
      actually lives: network clients, identity, the DB services, push, device attributes,
      kit mappings. Catching a regression at this gate is cheap; catching it at M4, after
      the hard tier has landed on top of it, is not.

**This is the checkpoint.** If scope has to give, it gives here — see "If the team gets
pulled away" below. A QA failure counts as not having reached the gate.

### M3 — gate: 78% · Hard tier, first half

3,753 LOC. `MessageManager` (1,089) · `KitConfiguration` (1,069) · `KitFrameworkWrapper`
(809) · `MParticleDBManager` (786).

- [ ] **Smoke QA** on the integration branch: event stream, identity, one kit, Rokt kit.
      Half an hour, not the full pass — QA pass 1 already covered the medium tier.

### M4 — gate: 100% · Internal Java = 0

4,853 LOC. `ConfigManager` (1,514) · `KitManagerImpl` (1,441) · `MPUtility` (858) ·
`MParticleJSInterface` (840, last file in) · `Logger` (200).

The tracker reads 100%. Every Java line the migration set out to move has moved, and the
API guard has confirmed at every step that no frozen contract went with it.

### M5 — Release

Not a percentage — M4 was the last one.

- [ ] **Manual QA pass 2 — full checklist (§7)**
- [ ] `scripts/api-guard.sh check` across the whole migration: confirm no frozen contract
      moved, and that every reviewed internal diff has its audit in the PR that made it
- [ ] Release

### Not part of the 100% gate

Deferred on purpose, not forgotten. None of it is needed for internal Java to reach zero,
and all of it is either delicate or zero-risk:

- **Lane I — facade thinning** (~5,000 LOC). The most delicate work in the project: moving
  `MParticle`, `MParticleOptions`, `CommerceEvent`, `MPEvent` and `IdentityApi` bodies into
  Kotlin while their declarations stay put. It moves the Kotlin % and not the goal metric,
  so it can follow the release.
- **`testutils`** (43 files) and the three remaining Java files in test source sets.
- **Cleanup** items.

### If the team gets pulled away

This will happen, and the plan is built for it. Every gate is a shippable state: the facade
split means the public API is unchanged at any percentage, and the ratchet means the number
never goes backwards while attention is elsewhere. Stopping at M2 and shipping is a real
outcome, not a failure.

If the migration has to be trimmed rather than paused, cut in this order and say so in the
team channel:

1. **`KitConfiguration` + `KitManagerImpl`** — 2,510 LOC, 11 points. Biggest and riskiest,
   referenced by 29 and 12 kit files respectively. Cutting these keeps the release safe.
2. **`MParticleJSInterface`** — 840 LOC, 4 points. Highest blast radius per line: the
   WebView bridge resolves its methods by name from JavaScript outside this repo.
3. **Lane G medium** — 1,236 LOC, 6 points. Lowest value per line, most platform-coupled.

Cutting all three still lands **79%**, with every easy and medium file converted and the
public API provably unchanged. That is a good outcome to stop on. Rushing the hard tier
past a QA pass to reach 100% is not.

## 7. Manual QA checklist

Two full passes and one smoke pass, against a real workspace, on a minSdk device and a
current-SDK device:

- **At M2 (61%) — full pass.** Straight after the medium tier, which is where most of the
  behaviour-carrying code lands. Cheap to fix here, expensive after the hard tier lands on
  top of it.
- **At M3 (78%) — smoke pass.** First four items only, half an hour, on the integration
  branch, to catch a systemic break from the first hard-tier files.
- **At M5 — full pass.** The release gate.

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

| Risk                                                                               | Mitigation                                                                                                                                                                                                                                            |
| ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A conversion silently tightens nullability and a customer's `null` starts throwing | The unannotated-param → `T?` rule; API guard diff; characterization tests                                                                                                                                                                             |
| A Kotlin class becomes `final` and breaks a kit author                             | Facade list keeps the kit ABI in Java; kit-compatibility CI job runs on every PR                                                                                                                                                                      |
| `MParticleJSInterface` method renamed by the converter                             | Converted last, JS bridge instrumented tests must pass, names frozen                                                                                                                                                                                  |
| `KitManagerImpl` reflective load breaks                                            | Class name and constructor arity are explicitly frozen; kit-compatibility job covers it                                                                                                                                                               |
| Merge conflicts across a 17k-LOC migration                                         | Directory-scoped lanes, Graphite stacks within a lane, batching confined to a single lane directory                                                                                                                                                   |
| Migration drags past the window                                                    | Ratchet stops the problem growing; the facade split means we can stop at any milestone and still be in a shippable, non-breaking state                                                                                                                |
| **Review throughput, not conversion speed, is the binding constraint**             | ~60–80 PRs, and three engineers who are also each other's reviewers. Named reviewer per lane, same-day turnaround, and mechanical conversions approved on evidence — green API guard, green tests, diff is a language port — rather than line-by-line |
| **Lane H runs in parallel instead of last**                                        | Lanes are file-scoped and Java/Kotlin interoperate in-module, so nobody is blocked. Residual risk is a same-day semantic conflict between a caller and an H callee; tests catch it, the guard does not. H's owner announces each merge                |
| **Batched easy-tier PRs hide a bad conversion**                                    | Only viable because the API guard reports exactly which signatures moved. A batch whose guard output is anything but "no diff" gets split before review                                                                                               |
