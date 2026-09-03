# mParticle Android SDK - Agent Instructions

This file holds only what an agent cannot get by reading the repository. Anything a config file
already states — plugin and dependency versions, SDK levels, ktlint rules, the kit list — is
deliberately absent, because the config cannot go stale and this file can. Read `build.gradle`,
`gradle.properties`, `.editorconfig`, `.trunk/trunk.yaml` and the `settings*.gradle` files instead.

Environment setup, the Maven-local workflow and the isolated-kit builds are in
[ONBOARDING.md](ONBOARDING.md). Commit and PR conventions are in
[CONTRIBUTING.md](CONTRIBUTING.md). API reference lives in the
[public docs](https://docs.mparticle.com/developers/sdk/android/).

## What this repo is

A public, multi-module Gradle library published to Maven Central, plus the integration kits that
forward events into third-party SDKs. It is not an app. Treat every `public` symbol as consumer
API: prefer additive changes and `@Deprecated` over removals, keep the APK-size impact of any
change in mind, and do not raise `android-core`'s `minSdk` — it is the advertised compatibility
floor, not a build detail.

## Repository shape (non-obvious parts only)

Gradle is split across several settings files, and a bare `./gradlew` invocation sees only the
first one:

| Settings file                   | Contains                                                                     |
| ------------------------------- | ---------------------------------------------------------------------------- |
| `settings.gradle`               | `android-core`, `android-kit-base`, `testutils`, `kit-plugin`, `tooling/*`   |
| `settings-kits.gradle`          | the kits under `kits/`; needs `-c settings-kits.gradle`                      |
| `settings-kit-examples.gradle`  | kit sample apps (CI only); applies `settings-kits.gradle` on top             |
| `settings-rokt-sdk-plus.gradle` | `rokt-sdk-plus`, held out of the main build so it publishes on its own creds |

- `build-logic/` is an included build; it supplies the `mparticle.android.library.publish` plugin
  that every publishable module applies.
- `tooling/custom-lint-rules` is shipped to consumer apps through `lintPublish` in
  `android-core/build.gradle`, so a change there runs inside other people's builds.
- `kits/matrix.json` drives the CI `build-kits` matrix and is a **separate list** from
  `settings-kits.gradle`. A new kit must be added to both or CI silently never builds it.
- Gradle project paths do not match directory paths for kits: `settings-kits.gradle` prefixes each
  kit group with `android-`, so `kits/braze/braze-41` is `:kits:android-braze:braze-41`.
  `settings.gradle` likewise renames `kit-plugin` to `android-kit-plugin`.

## Commands

JDK 17 — `gradle.properties` sets `JAVA_VERSION` and every CI job installs Zulu 17. ONBOARDING.md's
`JAVA_HOME` section still tells you to install Java 11; ignore that part of it.

- Core + tooling unit tests — `./gradlew test`
- Android lint — `./gradlew lint`; Kotlin lint — `./gradlew ktlintCheck`
- Instrumented tests — `./gradlew :android-core:cAT :android-kit-base:cAT --stacktrace`, needs an
  API 28 emulator
- Kit unit tests — `./gradlew -p kits testRelease -c ../settings-kits.gradle -Pmparticle.kit.mparticleFromMavenLocalOnly=true`
- Everything trunk gates — `trunk check`

### Command traps

1. **`./gradlew test` and `./gradlew build` never touch a kit.** Kits are a separate Gradle build.
   A green root build says nothing about anything under `kits/`.
2. **A Gradle test task that matched zero tests still passes.** `testutils` has no `src/test` at
   all — only `src/androidTest` — so `./gradlew :testutils:test` succeeds having run nothing. Read
   the test report, not the exit code, when you expect a specific test to have run.
3. **Unit tests are not the bulk of the suite.** `android-core` and `android-kit-base` both keep
   substantial suites under `src/androidTest`, which only `cAT` on an emulator executes. "Tests
   pass" is ambiguous here — say which of the two you ran.
4. **Some kits are commented out of `settings-kits.gradle`** because they need a newer Kotlin than
   the root plugin, and they build standalone from their own directory with their own wrapper. The
   commented-out lines name the current set; see
   [ONBOARDING.md](ONBOARDING.md#isolated-kits-different-kotlin-version) for the invocation. Adding
   another one also requires its own publish step in `.github/workflows/release-publish.yml` — the
   aggregate kit publish will not pick it up.
5. **Publish core to Maven local before building any kit, and always pass `-PVERSION`.** Kits
   resolve `android-core`/`android-kit-base` from `mavenLocal()` at an exact version. With no
   `-PVERSION` the version falls back to `0.0.0` and kit resolution fails with a confusing
   "could not find" error. CI derives it from the `VERSION` file into
   `ORG_GRADLE_PROJECT_VERSION`.
6. **`trunk check` does not lint everything.** `.trunk/trunk.yaml` has an `ignore` block that
   excludes an isolated kit directory, `**/gradlew` and one script from _all_ linters. Check it
   before assuming a file is covered.
7. **Kit lint is advisory in several kits.** Grep the kit `build.gradle` files for
   `abortOnError false`: in those modules `lint` prints findings and still exits 0. `android-core`
   sets `abortOnError true`, so core lint does fail the build.
8. **`android-core/lint-baseline.xml` is not a baseline.** Despite the name it is wired in as
   `lintConfig`, and its contents are a `<lint>` config suppressing `UnknownNullness` on an
   explicit path list. It will not absorb a newly introduced lint finding.
9. **`org.sonarqube` is applied in the root `build.gradle` but no workflow invokes it.** There is
   no Sonar analysis on a pull request. Do not treat it as a gate or wait for it.

## Pull requests

- Branch from and target `main`. A `development` branch still exists and is stale — do not use it.
- Conventional-commit PR titles (`feat`, `fix`, `chore`, `ci`, `docs`, …) are the house convention;
  [CONTRIBUTING.md](CONTRIBUTING.md) has the full type list. No CI job actually enforces the title
  or the branch name, so a malformed one will merge — match the convention anyway.
- The release version bump is a **manual** `bump-type` input on the `Release – Draft` workflow.
  Commit type does not decide it, whatever CONTRIBUTING.md implies about automatic releases.
- Merges are squash-only and need an approving review plus a CODEOWNERS review
  (`.github/CODEOWNERS` assigns everything to `@mParticle/sdk-team`). Force-pushing `main` is
  blocked, and commits GitHub cannot attribute to a user require an extra approval.
- **The required-check set is much narrower than the check list.** On `main` the repository ruleset
  currently requires only `Unit Tests`, `Lint Checks` and `Kotlin Lint Checks`. `Trunk code check`,
  every `Instrumented Tests (…)` job, `Security Lint Checks`, `Kit Compatibility Test` and
  `build-kits` all run and are reported but are **not** required, so a PR can merge with them red.
  Re-read the ruleset rather than trusting this list, and never say "CI is green" — name the job.
- Add a `CHANGELOG.md` entry for anything user-visible, under one of `Added`, `Changed`,
  `Deprecated`, `Fixed`, `Removed`, `Security`, in imperative mood. Never invent or auto-generate
  one; if the right wording is unclear, leave it for a human.

## Conventions no config enforces

- Kit modules ship their own `.editorconfig` files, some with `root = true`, which would otherwise
  shadow the repository one. The root `build.gradle` compensates by injecting
  `additionalEditorconfig` overrides for `kits/` paths only, so ktlint is deliberately stricter in
  core than in kits. Fix a kit inside the kit — never by relaxing the core rules.
- Every Android module must declare an explicit `namespace`; the root build fails the configuration
  phase with a named error if one is missing.
- Add comments only where the code cannot be made clearer. If you are reaching for a comment to
  explain confusing code, rewrite the code instead.
- Ask before you add a third-party dependency, change an SDK level, edit a ProGuard or
  consumer-ProGuard rule, or touch anything under `.github/` or `scripts/`. Each of those changes
  what consumer apps ship or how a release is cut, and none of them are reversible after publish.

## External references

- [mParticle Android SDK docs](https://docs.mparticle.com/developers/sdk/android/)
- [Rokt mParticle integration guide](https://docs.rokt.com/developers/integration-guides/rokt-ads/customer-data-platforms/mparticle/)
