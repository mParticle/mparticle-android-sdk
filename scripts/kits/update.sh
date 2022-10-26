#!/usr/bin/env bash
: "${1?"Kit Test Branch missing - usage: $0 \{branch_name\}"}"

CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`
# checkout a kit branch for testing
git branch -D "$1"
git checkout -b "$1"

# update kit references
git submodule foreach "git fetch; git reset --hard origin/master";

# commit kit reference deltas and push to private
git add kits/*;
git commit -m "Update submodules"
git push -f origin "$1"

git checkout "$CURRENT_BRANCH"
