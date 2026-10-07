# Java to Kotlin migration playbook

This playbook governs the conversion of `android-core` and `android-kit-base` internals from Java to
Kotlin. It exists because a Java-to-Kotlin conversion is not automatically compatible: a class becomes
`final`, a static moves behind a `Companion`, a `throws` clause disappears, a platform type becomes a
non-null parameter that rejects `null` at runtime. None of those show up in a source diff, all of them
break somebody downstream, and this SDK is consumed by Java apps, Kotlin apps and third-party kits that
compile against it.

The two rules that every other rule follows from:

1. **No breaking change.** The public API, its binary signatures, its nullability behaviour and every
   name-based contract stay exactly as they are for Java consumers, Kotlin consumers and kit authors.
2. **`main` is always releasable.** Every merged commit can be shipped through `Release – Draft`
   without follow-up work. The migration itself does not schedule releases; other work may need one at
   any time.

[TRACKER.md](TRACKER.md) lists the pull requests, their order and their status.

## Scope

**Converts to Kotlin.** Everything under `android-core/src/main` and `android-kit-base/src/main` that
is an implementation detail: the persistence layer, networking implementations, identity internals,
the event pipeline, the kit bridge, kit-base internals, platform helpers and the utility classes in
`com.mparticle.internal`. The unpublished `testutils` module can follow at any time.

**Stays Java.** Classes that customers or kit authors compile against keep their Java declarations in
the 6.x line, even where their bodies are later moved into Kotlin helpers:

- `com.mparticle`: `MParticle`, `MParticleOptions`, `MPEvent`, `BaseEvent`, `SdkListener`, `Session`,
  `Configuration`, `MParticleTask`, `AttributionResult`, `AttributionError`, `AttributionListener`,
  `MPService`, `MPReceiver`
- `com.mparticle.commerce`, `com.mparticle.consent`, `com.mparticle.media`, `com.mparticle.messaging`,
  `com.mparticle.segmentation`: every class
- `com.mparticle.identity`: `IdentityApi`, `IdentityApiRequest`, `IdentityApiResult`,
  `IdentityHttpResponse`, `IdentityStateListener`, `MParticleUser`, `AliasRequest`, `AliasResponse`,
  `BaseIdentityTask`, `TaskSuccessListener`, `TaskFailureListener`
- `com.mparticle.networking`: `NetworkOptions`, `DomainMapping`, `Certificate`, `MPUrl`,
  `MPConnection`, `BaseNetworkConnection`
- `com.mparticle.kits`: `KitIntegration`, `KitUtils`, `ReportingMessage`, `CommerceEventUtils`,
  `FilteredMParticleUser`, `FilteredIdentityApiRequest`
- every `package-info.java`

The reason is a property of the Kotlin language, not caution. A Kotlin caller that wrote
`identity.currentUser` against a Java getter keeps compiling only if the Kotlin replacement declares a
_property_; a caller that wrote `identity.getCurrentUser()` keeps compiling only if it declares a
_function_. No declaration satisfies both, so converting a class Kotlin consumers compile against is a
source-level break for one of them. Test doubles add a second constraint: the `com.mparticle.kit` Gradle
plugin (`kit-plugin/src/main/groovy/com/mparticle/kits/KitPlugin.groovy`) puts
`org.mockito:mockito-core:1.10.19` on every kit's test classpath unless the kit pins a newer version, and
kit tests mock `MParticle`, `MParticleOptions`, `FilteredMParticleUser`, `KitManagerImpl` and
`KitConfiguration`; Mockito 1.x cannot mock final classes. A Kotlin-first public API is a design task for a
future major version.

`KitConfiguration`, `KitManagerImpl` and `MParticleJSInterface` are visible to kit authors or resolved
by name and are decided individually, late, with the consumer fixtures as evidence; the decision is
recorded at the Phase 3 gate in [TRACKER.md](TRACKER.md).

## What "compatible" means here

| Tier               | Contract                                                                                                                                                                                                                                                                                                                                                        | Proven by                                                                                                                  |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **Binary surface** | Every class, member signature, `final`/`static`/`abstract` modifier, `throws` clause and nullability annotation that survives R8 in `android-core`, and every public member of `android-kit-base` (which is not minified).                                                                                                                                      | `scripts/api_compat_report.py` (japicmp against the last published release); `./gradlew apiCheck` (dump diff)              |
| **Kit ABI**        | `KitIntegration` and its nested listener interfaces, the kit-base helpers above, `KitManagerImpl`, `KitConfiguration`, and the internal classes kits import: `Logger`, `MPUtility`, `KitManager`.                                                                                                                                                               | Kit Compatibility Test and build-kits (every kit compiles against Maven Local); consumer fixtures                          |
| **Name-based**     | `Class.forName("com.mparticle.kits.KitManagerImpl")` and its four-argument constructor; kit classes loaded by name with a no-argument constructor; `@JavascriptInterface` method names; stack-frame class-name heuristics; `Configuration` class tokens; `Parcelable.CREATOR`; components registered in consumer manifests; third-party `Class.forName` probes. | Instrumented tests; the rule that strings, constructor arity and method names are copied verbatim; ProGuard diffs reviewed |
| **Test doubles**   | Classes that external tests mock stay non-final with non-final methods.                                                                                                                                                                                                                                                                                         | The API dump records `final`; kit unit tests run in build-kits                                                             |
| **Kotlin-source**  | Property-versus-getter syntax, platform types becoming explicit nullability, companion resolution through R8-processed metadata, the Kotlin metadata version consumers can read.                                                                                                                                                                                | The Kotlin consumer fixture compiled with warnings as errors against Maven Local; the kits as a real-world Kotlin fixture  |

Two facts about the shipped artifact matter for every conversion. The `android-core` AAR is
R8-minified with `-repackageclasses`, so the consumer-visible API is exactly what
`android-core/proguard.pro` keeps. And R8 keeps `kotlin.Metadata` on those classes but does not rewrite
it, so a companion object on a kept class needs its own keep rule or Kotlin consumers cannot resolve it.

## Conversion rules

Use the IDE's Java-to-Kotlin conversion, then apply this table before anything else. Convert; do not
refactor. Renames, coroutine adoption and null-safety improvements are separate follow-ups.

| Java construct                                                      | Kotlin form                                                                                                       | Why                                                                                              |
| ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Non-final class or public/protected method                          | `open class`, `open fun`, always, for anything in the binary surface or kit ABI                                   | Kotlin is final by default; consumers and their tests depend on non-finality                     |
| `public static` method                                              | `@JvmStatic` in a `companion object` or on an `object`                                                            | Otherwise Java sees `X.Companion.foo()`                                                          |
| `public static final` primitive or `String`                         | `const val`                                                                                                       | Stays a field                                                                                    |
| Other `static final` fields; public instance fields                 | `@JvmField`                                                                                                       | Otherwise the field becomes accessors and disappears from the surface                            |
| Companion object on a class `proguard.pro` keeps                    | A rules-only pull request adding `-keep class com.mparticle.X$Companion` lands first                              | R8 renames `X$Companion` and does not rewrite the metadata in the shipped AAR                    |
| Top-level functions or properties                                   | Not allowed in `src/main`                                                                                         | R8 strips `META-INF/*.kotlin_module`; Java sees an unexpected `FooKt` class                      |
| Getter/setter pairs on kit-visible types                            | Match the syntax existing Kotlin callers in this repository use; state the choice in the pull request             | Property versus function is source-breaking for the other style                                  |
| Unannotated parameter                                               | `T?`, always                                                                                                      | A non-null parameter emits `checkNotNullParameter`; Java callers and `Mockito.any()` pass `null` |
| Unannotated return type                                             | `T?` unless the body provably never returns `null`; for kit-visible types, audit callers in this repository first | `T?` is honest but breaks Kotlin callers that dereference a former platform type; record it      |
| `@NonNull` / `@Nullable`                                            | Mirror exactly                                                                                                    | These annotations are the contract                                                               |
| `throws`                                                            | `@Throws(X::class)`                                                                                               | Removing a checked exception breaks Java callers that catch it                                   |
| `synchronized` / `volatile`                                         | `@Synchronized` / `@Volatile`                                                                                     | Silently dropped otherwise                                                                       |
| `protected` member used from the same package                       | `internal`, or public with `@RestrictTo`                                                                          | Kotlin `protected` is not package-visible                                                        |
| Package-private class                                               | `internal class`                                                                                                  | JVM-public but excluded from the API dump; R8 renames it in core                                 |
| Static nested class                                                 | Nested class (not `inner`)                                                                                        | Same JVM name                                                                                    |
| Anonymous class                                                     | `object : Type { }`                                                                                               | Synthetic names change; nothing references them by name                                          |
| Overloads                                                           | Keep every overload explicitly; no default arguments on anything in the binary surface or kit ABI                 | Defaults add `DefaultConstructorMarker` overloads                                                |
| Generic signatures                                                  | `@JvmSuppressWildcards` / `@JvmWildcard` until the dump matches                                                   | Kotlin inserts `? extends` where Java had none                                                   |
| Value or model classes                                              | Plain `class`, never `data class`                                                                                 | `data class` changes `toString()` and adds `componentN`/`copy`                                   |
| Interfaces                                                          | No default bodies                                                                                                 | The Java interfaces have none; bodies create `DefaultImpls` under the current compiler settings  |
| Reflection strings, constructor arity, `@JavascriptInterface` names | Copy verbatim                                                                                                     | No compiler checks them                                                                          |
| Enum                                                                | `enum class` with the same constants                                                                              | Kotlin adds `getEntries()`, a harmless addition                                                  |

Convert in place: `Foo.java` becomes `Foo.kt` in the same directory. Both `src/main/java` and
`src/main/kotlin` already compile Kotlin in every module, and a directory move on top of a conversion
hides signature changes in review.

## Pull request checklist

Run locally before requesting review. CI runs the same set.

- [ ] `./gradlew apiCheck` passes. If the dump changed, `scripts/check_api_dump.py --base origin/main`
      reports no frozen class, and the reviewable diff is pasted into the description with the reason.
- [ ] The **Binary Compatibility** job (`scripts/api_compat_report.py`) reports no incompatible change
      against the last published release.
- [ ] `./gradlew test ktlintCheck lint` pass, and the unit-test report shows the same test count as
      `main`.
- [ ] Instrumented tests for the module touched pass: `./gradlew :android-core:cAT` or
      `./gradlew :android-kit-base:cAT` on an API 28 emulator.
- [ ] Kit compatibility passes against Maven Local: `./gradlew publishMavenPublicationToMavenLocal`,
      then `./gradlew -p kits testRelease -c ../settings-kits.gradle -Pmparticle.kit.mparticleFromMavenLocalOnly=true`,
      the isolated kits, and `./gradlew -c settings-compat.gradle assembleDebug`.
- [ ] `trunk check` is clean.
- [ ] The description lists every parameter or return type whose nullability was decided by hand;
      every `open`, `@JvmStatic`, `@JvmField`, `@Throws`, `@Synchronized` or `@Volatile` added; and
      confirms that reflection strings and ProGuard files are unchanged (or explains the diff).
- [ ] The SDK Size Impact comment shows a change of at most a few kilobytes, or the description
      explains it.
- [ ] The risk class (below) is stated in the first line of the description.

A mechanical conversion is approved on this evidence plus a read of the nullability decisions, not on
a line-by-line reading of a language port.

## Risk classes and merge rules

The release is never the gate; the gate is applied at merge time so that `main` stays releasable.

| Class | Meaning                                                                                                 | Merge rule                                                                                                                                                                            |
| ----- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **L** | Mechanical conversion or build change; zero public-surface diff; covered by unit and instrumented tests | The checklist above                                                                                                                                                                   |
| **M** | Behaviour-carrying internals: network, persistence, handlers, device attributes, kit bridge             | The checklist, plus the stack's QA smoke before the next M-class pull request in the same stack merges                                                                                |
| **H** | Touches ProGuard, a kit-ABI class, a name-based contract, or a class that several kits import           | The ProGuard protocol below before merge; merge only when no release is being prepared; let the post-push instrumented and kit jobs on `main` run green before the next H-class merge |

### ProGuard protocol

Applies to any pull request that edits `android-core/proguard.pro` or a consumer ProGuard file, and to
conversions that need a new keep rule. A ProGuard mistake is the one class of change that can break
customers while every compiler and unit test stays green.

1. **Rules-only.** Code and rules never share a pull request. A conversion that needs a keep lands
   _after_ the rules-only pull request that adds it; the keep is harmless while the class is still
   Java.
2. **Prove the artifact did not move.** Attach a before/after diff of the release AAR's class list and
   of `javap -public` on every kept class, the japicmp result, and the `-printusage` /
   `-printconfiguration` diffs.
3. **Compile every consumer.** All kits from Maven Local, both consumer fixtures, kit unit tests.
4. **Run a minified consumer app end to end.** Build the `size-report` release APK against the new
   AAR, install it on an emulator, initialise the SDK, send an event, confirm the upload.
5. **Merge in a quiet window.** Not while a release is being prepared, and not until the previous
   ProGuard change has been on `main` long enough for the post-push jobs to run green.

Do not add `-keep class kotlin.Metadata { *; }` speculatively. Whether Kotlin consumers can resolve
companion objects in the shipped AAR is established by the Kotlin consumer fixture first; if they
cannot, the metadata keep is a defect fix and goes through this protocol on its own.

### Rollback

Every pull request is one squash commit. Reverting a mid-stack conversion is a single `git revert`,
provided later pull requests in the same stack are reverted first. ProGuard pull requests revert
independently because they never share a commit with code.

### Releasing while the migration is in flight

Whoever runs `Release – Draft` for unrelated work while migration pull requests are unreleased on
`main` adds these steps:

- Confirm the Binary Compatibility job on the release commit is green.
- Confirm the post-push instrumented, Kit Compatibility Test and build-kits jobs on the release commit
  are green (name the jobs).
- If an H-class pull request is unreleased, run step 4 of the ProGuard protocol once on the release
  candidate.
- Add a `CHANGELOG.md` line under _Changed_ for each unreleased ProGuard change. Pure conversions do
  not get changelog entries.
- Do not merge an H-class pull request between opening the release pull request and publishing.

## Pull request conventions

- One concern per pull request, based on `main`. Within a stack, pull request N+1 targets pull request
  N's branch until N merges, then retargets `main`. Stacks share no files and never depend on each
  other. Keep each pull request under roughly 500 changed lines outside pure renames.
- Build and improvement changes are never in a conversion stack. A conversion that needs a build change
  depends on the merged build pull request.
- Titles follow the house convention: `refactor(core): convert internal.database tables to Kotlin`,
  `build: …`, `ci: …`, `test: …`. A pure conversion has no `CHANGELOG.md` entry.
- A pull request that adds Java to `android-core/src/main` or `android-kit-base/src/main` needs a
  stated reason; the direction of travel is Kotlin.
