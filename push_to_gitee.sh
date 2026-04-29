#!/usr/bin/env bash
set -euo pipefail

GITEE_REMOTE="gitee"
GITEE_URL="git@gitee.com:Oterman/zrun-android.git"
BRANCH="master"

# 删除已有的 gitee remote（若存在）
if git remote get-url "$GITEE_REMOTE" &>/dev/null; then
  echo "删除已有 remote: $GITEE_REMOTE"
  git remote remove "$GITEE_REMOTE"
fi

# 添加新的 SSH remote
echo "添加 remote: $GITEE_REMOTE -> $GITEE_URL"
git remote add "$GITEE_REMOTE" "$GITEE_URL"

# 推送 master 分支
echo "推送 $BRANCH 到 $GITEE_REMOTE ..."
git push "$GITEE_REMOTE" "$BRANCH"

echo "完成。"
