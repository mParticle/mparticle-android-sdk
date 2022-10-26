#!/usr/bin/env bash
: ${1?"Version missing - usage: $0 x.y.z"}  # nosemgrep

#update build.gradle
sed -i '.bak' "s/version = '.*-SNAPSHOT/version = '$1-SNAPSHOT/g" build.gradle

#update README.md
sed -i '.bak' "s/'com.mparticle:android-core:.*'/'com.mparticle:android-core:$1'/g" README.md
sed -i '.bak' "s/'com.mparticle:android-example-kit:.*'/'com.mparticle:android-example-kit:$1'/g" README.md
sed -i '.bak' "s/'com.mparticle:android-another-kit:.*'/'com.mparticle:android-another-kit:$1'/g" README.md

#commit the version bump, tag, and push to private and public
git add build.gradle
git add README.md
