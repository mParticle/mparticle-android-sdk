# Release Process

This document outlines the process for releasing the mParticle Android SDK and its kits.

The Android SDK and kits are released together from this monorepo using GitHub Actions.

## Release workflows

### Release - Draft

Use `Release - Draft` to prepare a release PR. The workflow:

1. Reads the current version from `VERSION`.
2. Computes the next release version from `bump-type` and optional `qualifier`.
3. Writes the computed version back to `VERSION`.
4. Publishes to Maven local as a smoke test.
5. Generates the changelog entry.
6. Opens a PR back into the branch selected in the workflow dispatch UI.

Generated release PR branches use `release/prep-<version>`, for example `release/prep-6.0.0-rc1`.

### Release - Publish

`Release - Publish` runs when `VERSION` changes on:

- `main`
- `workstation/*`

The workflow publishes the SDK and kits to Maven Central using the exact version in `VERSION`, then creates a GitHub release with the same tag.

## Stable releases

1. Run `Release - Draft` from the target branch.
2. Choose `patch`, `minor`, or `major` for `bump-type`.
3. Leave `qualifier` empty.
4. Review, approve, and merge the generated release PR.
5. Confirm `Release - Publish` succeeds.

Stable releases are marked as the latest GitHub release.

## Alpha, beta, and RC releases

Use a qualified release when a partner or internal validation flow needs a fixed Maven Central version before the stable release, for example `6.0.0-alpha1`, `6.0.0-beta1`, or `6.0.0-rc1`.

1. Run `Release - Draft` from the target branch, such as `workstation/6.0-Release`.
2. Choose the base `bump-type`:
    - `patch`, `minor`, or `major` to bump the base version.
    - `none` to keep the base version unchanged and only change or remove the qualifier.
3. Set `qualifier` to the pre-release identifier without the leading hyphen, for example `alpha1`, `beta1`, or `rc1`.
4. Review, approve, and merge the generated release PR.
5. Confirm `Release - Publish` succeeds.

Qualified releases are published to Maven Central with the exact qualified version. Their GitHub releases are marked as pre-releases and are not marked as latest.

The qualifier must start with an alphanumeric character and may only contain alphanumerics, dots, and hyphens.

## Version examples

- `5.78.2` with `bump-type=minor` and `qualifier=alpha1` produces `5.79.0-alpha1`.
- `5.79.0-alpha1` with `bump-type=none` and `qualifier=beta1` produces `5.79.0-beta1`.
- `5.79.0-beta1` with `bump-type=none` and `qualifier=rc1` produces `5.79.0-rc1`.
- `5.79.0-rc1` with `bump-type=none` and an empty `qualifier` produces `5.79.0`.

The draft workflow rejects any input combination that would produce the same version already stored in `VERSION`.

## Post-release verification

After a successful publish workflow, verify:

1. The GitHub release exists with the expected tag.
2. The SDK artifact is present in [Sonatype](https://central.sonatype.com/publishing).
3. The kit artifacts are present in [Sonatype](https://central.sonatype.com/publishing).
4. Pre-release tags are marked as pre-releases and stable tags are marked as latest.

## Troubleshooting

If release validation fails:

- Check the generated release PR for the computed `VERSION` and changelog changes.
- Check the Maven local smoke-test step in `Release - Draft`.
- Check the Maven Central publish steps in `Release - Publish`.
- For emulator-related validation issues, check `scripts/install-start-emulator.sh` and the GitHub Actions logs.
