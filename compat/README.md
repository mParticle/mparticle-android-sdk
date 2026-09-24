# Consumer compatibility fixtures

`java-consumer` and `kotlin-consumer` are small, unpublished `com.android.library` modules that
compile against the published `com.mparticle:android-core` (R8-minified) and
`com.mparticle:android-kit-base` artifacts the way an integrating app or a third-party kit does:
one from Java, one from Kotlin. They contain no tests and are never executed. Their only job is to
fail to compile when the shipped API changes in a way the module build in `settings.gradle` cannot
see, such as a class or companion object dropped by R8, a nullability change that becomes a Kotlin
error, or a checked exception added to a public signature.

## Why they resolve from mavenLocal

Both modules resolve `com.mparticle` exclusively from `mavenLocal()`, at the version in `VERSION`
(or `-PVERSION` when set), exactly as kits do through `KitPlugin.configureRepositories`. Resolving
from Maven Central would silently compile against the previous release instead of the one under
review. The Kotlin module compiles with `allWarningsAsErrors`, so a nullability or deprecation
warning against the published API fails the build.

## Running locally

Publish core and kit-base to mavenLocal first, then build the fixtures:

```bash
./gradlew publishMavenPublicationToMavenLocal -PVERSION="$(head -n 1 VERSION)"
./gradlew -c settings-compat.gradle assembleDebug -PVERSION="$(head -n 1 VERSION)"
```

CI runs the second command in the `Kit Compatibility Test` job of
`.github/workflows/pull-request.yml`, after core has been published to mavenLocal in that job.
