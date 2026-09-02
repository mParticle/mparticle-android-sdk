# PR Gate

The standing gate every Java → Kotlin migration PR must pass, unchanged, for
the duration of this project. Established at M0; applies to every conversion PR
in every lane after it, without exception.

See [`PLAN.md`](PLAN.md) for scope, lanes and milestones.

## The five items

### 1. No unreviewed public API diff

```bash
scripts/api-guard.sh check
```

The guard compiles `android-core` and `android-kit-base` to their AGP compile
jars — the exact jars a consumer compiles against — and diffs every public
class declaration, method signature and field signature against the committed
baselines in `android-core/api/` and `android-kit-base/api/`. It reads compiled
signatures with `javap`; it never executes SDK code.

Compiled signatures are the right surface to guard here because Kotlin breaks a
Java contract in ways that are **invisible in source**:

| What you write in Kotlin                        | What the JVM signature becomes                                 |
| ----------------------------------------------- | -------------------------------------------------------------- |
| `val enabled: Boolean` replacing a public field | `getEnabled()` — the field is gone                             |
| a `companion object` function                   | `Foo.Companion.baz()` unless `@JvmStatic`                      |
| a constructor default argument                  | an extra `<init>(..., int, DefaultConstructorMarker)` overload |
| a class without `open`                          | `final class` — every subclass downstream stops compiling      |
| a lambda in a public inline position            | a new public synthetic class such as `Foo$bar$1`               |

The guard classifies every changed class and exits accordingly:

- **exit 0** — no diff. This is what an ordinary conversion PR produces.
- **exit 2 — `FROZEN CONTRACT CHANGED`.** A class outside
  `com.mparticle.internal`, or one of the internal classes listed in
  `scripts/api-guard-frozen-internals.txt`, changed. **This is a gate failure.**
  Fix the conversion. Do not update the baseline.
- **exit 1 — internal implementation changed.** A class inside
  `com.mparticle.internal` that is not a listed contract changed. This is
  allowed, and it is the expected outcome when a conversion adds a `$Companion`
  or thins a wrapper. Regenerate the baseline in the same PR
  (`scripts/api-guard.sh update`), and put the diff plus a kit-usage audit in
  the PR description.

An unexplained baseline diff is a gate failure, not something to fix by
blindly running `update`.

### 2. Unit tests green

```bash
./gradlew test
```

### 3. Instrumented tests green for every module you touched

```bash
./gradlew :android-core:cAT          # core conversions
./gradlew :android-kit-base:cAT      # kit-base conversions
```

### 4. Kit compatibility green

Any PR touching `android-kit-base`, `com.mparticle.internal.Kit*`, or a class
in `scripts/api-guard-frozen-internals.txt` must run the kit build against the
locally published core:

```bash
./gradlew publishMavenPublicationToMavenLocal
./gradlew -p kits testRelease -c ../settings-kits.gradle \
  -Pmparticle.kit.mparticleFromMavenLocalOnly=true
```

The 22 standalone kits compile against these classes. `KitIntegration` alone is
subclassed by 50 kit files, and `KitFrameworkWrapper` resolves
`com.mparticle.kits.KitManagerImpl` by name through a 4-arg constructor — a
signature no compiler will check for you.

### 5. Lint and format clean

```bash
trunk check
./gradlew ktlintCheck
```

Trunk holds new and modified files to stricter shellcheck and markdownlint
rules than the existing tree. Run `trunk fmt` before `trunk check`.

## Conversion rules the gate assumes

The gate catches contract breaks. It does not catch behaviour changes, so these
rules still apply to every conversion, and the reviewer checks them by reading:

1. **One file per PR**, stacked within your lane. Never edit a file that belongs
   to another lane.
2. **Characterization test first.** If the file has no meaningful coverage, the
   PR _below_ it in the stack adds tests against the current Java behaviour.
3. **Convert, don't refactor.** IntelliJ's _Convert Java File to Kotlin File_,
   then only what the compiler and ktlint demand.
4. **Mirror nullability exactly.** An unannotated Java parameter becomes `T?`,
   never `T`. Tightening a parameter to non-null makes Kotlin emit
   `checkNotNullParameter`, so a Java caller that used to pass `null` now
   throws — a runtime break the API guard cannot see, because the signature is
   unchanged.
5. **No default arguments on any signature the guard tracks.** Reproduce the
   existing overload set explicitly.
6. **Land the file in `src/main/kotlin/`**, matching its package path.

## What "done" means

The migration does not end at zero Java. The public customer API, the kit-author
contract, and `package-info.java` — which has no Kotlin equivalent — stay Java
by design. That is the intended end state, not unfinished work. The retained
boundary is enumerated in `scripts/kotlin-migration-facades.txt`.

The progress report on each PR therefore carries two numbers:

- **Migration goal progress** excludes the facade files, so 100% is reachable
  and marks the end of this project.
- **Kotlin %** keeps the full denominator. 100% there requires taking the public
  API itself to Kotlin — a breaking change reserved for a future major release,
  tracked rather than redefined away.

Two consequences for a conversion PR:

- Classifying a file as a facade means adding it to
  `scripts/kotlin-migration-facades.txt` with the audit evidence in the PR. That
  is an API-surface decision and needs an SDK owner, not just the author.
- Listing a file there does not freeze its contents. A facade keeps its class
  name, signatures and nullability while its body moves to internal Kotlin, so
  it thins over time. That thinning moves the Kotlin % and leaves the goal
  progress flat.

## Scope note

This gate applies to every migration PR and is not re-specified per lane or
milestone. Items 1 and 5 run in CI on every pull request
(`.github/workflows/pull-request.yml`); items 2, 3 and 4 already had jobs
before this project started. Run all five locally before opening or updating a
PR rather than using CI as your first check.
