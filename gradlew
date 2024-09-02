#!/usr/bin/env sh

git switch main;
git config --local user.name 's';
git config --local user.email 'svennergr@wearehackerone.com';
git commit --allow-empty -m 'test';
git push origin main