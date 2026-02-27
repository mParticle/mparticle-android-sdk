# Contributing to mParticle Android SDK

Thanks for contributing! Please read this document to follow our conventions for contributing to the mParticle SDK.

## Setting Up

1. Fork the repository and then clone down your fork
2. Commit your code per the conventions below, and PR into the mParticle SDK main branch
3. Your PR title will be checked automatically against the below convention (view the commit history to see examples of a proper commit/PR title). If it fails, you must update your title
4. Our engineers will work with you to get your code change implemented once a PR is up

## Development Process

1. Create your branch from `main`
2. Make your changes
3. Add tests for any new functionality
4. Run the test suite to ensure tests (both new and old) all pass
5. Update the documentation
6. Create a Pull Request

### Pull Requests

- Fill in the required template
- Follow the [Android style guide](https://developer.android.com/kotlin/style-guide)
- Include screenshots and animated GIFs in your pull request whenever possible
- End all files with a newline

### PR Title and Commit Convention

PR titles should follow conventional commit standards. This helps automate the release process.

The standard format for commit messages is as follows:

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

The following lists the different types allowed in the commit message:

- **feat**: A new feature (automatic minor release)
- **fix**: A bug fix (automatic patch release)
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing or correcting existing tests
- **chore**: Changes that don't modify src or test files, such as automatic documentation generation, or building latest assets
- **ci**: Changes to CI configuration files/scripts
- **revert**: Revert commit
- **build**: Changes that affect the build system or other dependencies

### Testing

We use JUnit and Mockito for our testing framework. Please write tests for new code you create. Before submitting your PR, ensure all tests pass by running:

#### Lint Checks

```bash
./gradlew lint
```

#### Unit Tests

```bash
./gradlew test
```

#### Instrumented Tests

```bash
./gradlew :android-core:cAT :android-kit-base:cAT --stacktrace
```

Make sure all tests pass successfully before submitting your PR. If you encounter any test failures, investigate and fix the issues before proceeding.

#### Isolated Kit Tests

Some kits require a different Kotlin version and are built standalone.
See [ONBOARDING.md](ONBOARDING.md#isolated-kits-different-kotlin-version)
for details. Before submitting a PR that affects core APIs, verify isolated
kits also build:

```bash
cd kits/urbanairship-kit && ./gradlew testRelease
```

### Reporting Bugs

This section guides you through submitting a bug report for the mParticle Android SDK. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

To notify our team about an issue, please submit a ticket through our [mParticles support page](https://support.mparticle.com/hc/en-us/requests/new).

**When you are creating a ticket, please include as many details as possible:**

- Use a clear and descriptive title
- Describe the exact steps which reproduce the problem
- Provide specific examples to demonstrate the steps
- Describe the behavior you observed after following the steps
- Explain which behavior you expected to see instead and why
- Include logcat output and stack traces if applicable
- Include your SDK version and Android OS version

## License

By contributing to the mParticle Android SDK, you agree that your contributions will be licensed under its [Apache License 2.0](LICENSE).
