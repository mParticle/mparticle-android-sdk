#!/usr/bin/env bash
: "${2?"Kit Test Branch missing - usage: $0 \{branch_name\} \{target_branch\}"}"

CURRENT_BRANCE=`git rev-parse --abbrev-ref HEAD`
echo "Fetch"
git fetch origin
echo "Rebase"
git rebase origin/$1
echo "Push Updated Branch"
git push origin
echo "Delete Kit Feature branch Locally"
git branch -D $1
echo "Push Delete remote"
git push origin --delete $1
git checkout $CURRENT_BRANCE