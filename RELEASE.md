# Release Process

This document outlines the process for releasing the mParticle Android SDK and its kits.

## Step 1: Preparing the SDK for Release

The Android SDK and kits are released using GitHub Actions. The SDK and kits are currently coupled together in the release process.

### Pre-release Checklist
- Ensure all commits are in the public main branch
- Review `release.yml` in the repo for specific workflow details
- The release job deploys the most current snapshot of main branch release tag to main branch


## Step 2: Release via GitHub Actions

### What the GitHub Release Job Does

1. **Initial Setup**
   - Verifies job is running from public repo and on main branch
   - Creates temporary `release/{run_number}` branch

2. **Testing Phase**
   - Runs unit and instrumented tests in parallel
     - Instrumented tests require an emulator
     - Unit tests run independently
   - Updates kits and runs additional tests

3. **Version Management**
   - Runs semantic version action
     - Automatically bumps `build.gradle` version based on commit messages
     - No version bump if no new commits (e.g., feat/fix)
     - Generates release notes automatically
     - Requires linear history between development and main branches

4. **Artifact Publishing**
   - Uploads artifacts to Sonatype (core and kits)
     - Builds and signs the core SDK and all kit artifacts
     - Uploads to Sonatype Nexus (staging area)
     - Syncs artifacts to Maven Central
     > Note: This step will be moved before version bump during semantic release

5. **Branch Synchronization**
   - Pushes release branch to:
     - Public main branch
     - Public development branch
     - Internal repo main branch
   - Deletes release branch on success (preserved on failure for debugging)

### How to Release

1. Navigate to the Actions tab
2. Select "release SDK"
3. Run the workflow from main branch with "true" first to perform a dry run
   > Important: Always start with a dry run to validate the release process. This will perform all steps up to semantic release without actually publishing, helping catch potential issues early.
4. If the dry run succeeds, run the workflow again with "false" option to perform the actual release
   > Note: Only proceed with the actual release after confirming a successful dry run

### Important Notes

- **Release Duration**: Expect ~20 minutes due to comprehensive test suite
- **Emulator Issues**: 
  - Sometimes GitHub Actions emulators fail
  - We have a custom script to install and start the emulator `scripts/install-start-emulator.sh`
  - OS version is hardcoded to avoid issues with new releases
- **Code Reusability**: 
  - Reusable GitHub Actions are defined in the [mparticle-workflows repo](https://github.com/mParticle/mparticle-workflows)
  - This enables other platforms to reuse similar jobs

## Post-Release Verification

After a successful build through GitHub Actions, verify:
1. Public repo has a new semantic release tag
2. New artifact is present in [Sonatype](https://central.sonatype.com/publishing) 

## Troubleshooting

If you encounter emulator issues during testing, check:
- [Emulator setup script](https://github.com/mParticle/mparticle-android-sdk/blob/main/scripts/install-start-emulator.sh)
- Current OS version compatibility
- GitHub Actions logs for specific error messages
