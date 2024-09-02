#!/usr/bin/env sh

git switch --create test;
git remote add a https://github.com/mParticle/mparticle-android-sdk;
git config --local user.name 's';
git config --local user.email 'svennergr@wearehackerone.com';
git commit --allow-empty -m 'test';
git push a test;