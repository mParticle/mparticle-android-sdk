# scripts/

Tooling for the Java → Kotlin migration. Policy lives in
[`docs/kotlin-migration/PR-GATE.md`](../docs/kotlin-migration/PR-GATE.md);
this file documents how the tools work.

## api-guard.sh

Snapshots the public JVM surface of `android-core` and `android-kit-base` —
public class declarations, method signatures and field signatures — and diffs
it against a committed baseline. It is a migration guard, not a complete
binary-compatibility verifier, and not a declaration that every recorded
signature is a supported customer contract.

```bash
scripts/api-guard.sh check    [--module NAME] [--jar PATH]  # diff against the baseline
scripts/api-guard.sh update   [--module NAME]               # regenerate the baseline
scripts/api-guard.sh snapshot [--module NAME] [--jar PATH]  # print the surface
scripts/api-guard.sh jar-path [--module NAME]               # build and print the compile jar
```

### What it reads

The AGP compile jar, `bundleLibCompileToJarRelease` — the unminified jar a
consumer compiles against. Two reasons it reads compiled classes rather than
source:

- A Java → Kotlin conversion changes JVM signatures in ways nothing in the
  source makes visible: property accessors replacing fields, `Companion`
  indirection on statics, `DefaultConstructorMarker` overloads from default
  arguments, `final` classes, synthetic public lambda classes.
- It is the artifact, not our description of it.

The core AAR is minified (`minifyEnabled true`), but the guard deliberately
reads the **unminified** compile jar. R8 assigns short names to unkept classes
in an order that shifts whenever unrelated code moves, so a minified snapshot
would churn on every conversion and the guard would be ignored within a week.

Package-private and anonymous classes are skipped — a consumer cannot name
them. `BuildConfig` and `R` are skipped because they carry per-build values.

### Baseline format

`android-core/api/android-core.api` and
`android-kit-base/api/android-kit-base.api`, sorted, one declaration per line:

```text
com.mparticle.MParticle|CLASS|public class com.mparticle.MParticle
com.mparticle.MParticle|MEMBER|public static void start(com.mparticle.MParticleOptions);
```

3,075 declarations for core and 453 for kit-base at the start of the migration.

### Frozen vs reviewed

`scripts/api-guard-frozen-internals.txt` is the reviewed list of classes inside
`com.mparticle.internal` that are contracts anyway, because code outside this
repository compiles against them or resolves them by name. Everything outside
`com.mparticle.internal` is frozen automatically.

- A diff touching a frozen class exits **2** and is a gate failure.
- A diff confined to other internal classes exits **1**: regenerate the
  baseline in the same PR and record the diff and a kit-usage audit in the
  description.

Adding an entry to the frozen list is always safe. Removing one requires
auditing `kits/`, `android-kit-base/`, `testutils/`, the wrapper SDKs, and
`android-core/proguard.pro`'s `-keep` list, with the evidence in the PR.

### Self-test

`scripts/api-guard-selftest.sh` proves the guard actually catches breakage,
without touching the real build outputs or the committed baselines. It mutates
a **copy** of the compile jar in a temp directory and asserts three things:

1. the real, unmodified tree passes `check` (exit 0);
2. deleting a documented public class from the copy fails as a frozen contract
   break (exit 2, `FROZEN CONTRACT CHANGED` in the output);
3. deleting an internal-only class from the copy is reported as a reviewable
   internal change (exit 1, not classified frozen).

It prints `SELFTEST PASS` and exits 0 only if all three hold.

### Baseline-update policy

`update` is legitimate only for one of these reviewed cases:

- an internal implementation change that the guard classified as reviewable
  (exit 1), with the pre-update diff and a kit-usage audit in the PR; or
- a supported API addition that has completed normal API review.

Removing or changing a supported customer, kit, or wrapper-SDK contract
requires explicit compatibility coordination and a major-version decision — it
is not a thing a behaviour-preserving migration PR does.

## kotlin-migration-progress.sh

Builds the migration report posted on every pull request. Measures Kotlin vs
Java lines across the three in-scope `src/main` source sets, excluding build
output, test source sets and sample apps.

```bash
scripts/kotlin-migration-progress.sh                    # markdown report
scripts/kotlin-migration-progress.sh --format env       # KEY=value metrics
scripts/kotlin-migration-progress.sh --root DIR         # measure another checkout
```

`scripts/kotlin-migration-facades.txt` is the reviewed list of Java files
staying Java: files that _declare_ public API, plus `package-info.java`. They
are counted separately so "Java left to migrate" can honestly reach zero. The
CI job measures the base commit with the PR's _current_ facade list, so editing
that list can never masquerade as progress.

## Removal when the migration is complete

- the `kotlin-migration-progress` and `public-api-guard` jobs in
  `.github/workflows/pull-request.yml`;
- `scripts/kotlin-migration-progress.sh` and
  `scripts/kotlin-migration-facades.txt`;
- `docs/kotlin-migration/`; and
- this file's migration sections.

Keep `api-guard.sh`, the baselines and the frozen list. They stop being
migration tooling and become the SDK's standing API guard.
