#!/usr/bin/env bash
# 生成 patch 文件并打包到 rundemo/src/main/assets/
# 使用方式：在项目根目录执行 bash generate_patches.sh
set -euo pipefail

REMOTE="gitee"
BRANCH="master"
ASSETS_DIR="rundemo/src/main/assets"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APPLY_SCRIPT="$SCRIPT_DIR/apply_patches.sh"
TMP_DIR="$SCRIPT_DIR/.patches_tmp"

echo "========================================"
echo "  Patch 生成脚本"
echo "========================================"

# ── [1/5] 拉取最新远端 ─────────────────────
echo ""
echo "[1/5] 从 $REMOTE 拉取最新代码..."
git fetch "$REMOTE"

# ── [2/5] Rebase 到云端最新 ───────────────
echo ""
echo "[2/5] Rebase 到 $REMOTE/$BRANCH..."
if ! git rebase "$REMOTE/$BRANCH"; then
  echo ""
  echo "错误：Rebase 失败，可能存在冲突。"
  echo "请手动解决冲突后执行："
  echo "  git rebase --continue"
  echo "或放弃本次 rebase："
  echo "  git rebase --abort"
  git rebase --abort 2>/dev/null || true
  exit 1
fi

# ── [3/5] 统计未推送提交 ──────────────────
echo ""
echo "[3/5] 检查未推送的提交..."
COMMIT_COUNT=$(git rev-list "$REMOTE/$BRANCH"..HEAD --count)

if [ "$COMMIT_COUNT" -eq 0 ]; then
  echo "  没有未推送的提交，无需生成 patch。"
  exit 0
fi

echo "  找到 $COMMIT_COUNT 个未推送的提交："
git log "$REMOTE/$BRANCH"..HEAD --oneline

# ── [4/5] 生成 patch 文件 ─────────────────
echo ""
echo "[4/5] 生成 patch 文件..."
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

git format-patch "$REMOTE/$BRANCH"..HEAD -o "$TMP_DIR"

# 将两个脚本一起打包进去
cp "$SCRIPT_DIR/generate_patches.sh" "$TMP_DIR/generate_patches.sh"

if [ ! -f "$APPLY_SCRIPT" ]; then
  echo "错误：找不到 apply_patches.sh，请确保它与本脚本在同一目录。"
  rm -rf "$TMP_DIR"
  exit 1
fi
cp "$APPLY_SCRIPT" "$TMP_DIR/apply_patches.sh"

echo "  Patch 目录内容："
ls -1 "$TMP_DIR"

# ── [5/5] 打包并拷贝到 assets ─────────────
echo ""
echo "[5/5] 打包并复制到 $ASSETS_DIR..."
ZIP_NAME="patches_$(date +%Y%m%d_%H%M%S).zip"
ZIP_PATH="$SCRIPT_DIR/$ZIP_NAME"

# 进入 tmp 目录的父目录，保持 zip 内路径简洁
cd "$SCRIPT_DIR"
zip -r "$ZIP_NAME" "$(basename "$TMP_DIR")" -x "*.DS_Store"
mv "$ZIP_PATH" "$SCRIPT_DIR/$ASSETS_DIR/$ZIP_NAME"
rm -rf "$TMP_DIR"

echo ""
echo "========================================"
echo "  完成！"
echo "  ZIP 已保存到：$ASSETS_DIR/$ZIP_NAME"
echo "  包含 $COMMIT_COUNT 个 patch 文件 + 两个脚本"
echo "========================================"
