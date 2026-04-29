#!/data/data/com.termux/files/usr/bin/bash
# 使用方法：
#   1. 将此文件复制到 Termux：cp /sdcard/Download/termux_push_from_zip.sh ~/push_from_zip.sh
#   2. chmod +x ~/push_from_zip.sh
#   3. 运行：~/push_from_zip.sh
#
# 桌面快捷方式（Termux:Widget）：
#   mkdir -p ~/.shortcuts && cp ~/push_from_zip.sh ~/.shortcuts/推送到Gitee.sh

set -e

# ── 配置区（可按需修改）────────────────────────
DOWNLOAD_DIR="/sdcard/Download"
GITEE_URL="git@gitee.com:Oterman/zrun-android.git"
BRANCH="master"
WORK_DIR="$HOME/zip_push_tmp"
# ─────────────────────────────────────────────

echo "========================================"
echo "  Gitee 一键推送脚本"
echo "========================================"

echo ""
echo "[1/4] 查找最新 DemoHub ZIP..."
ZIP=$(ls -t "$DOWNLOAD_DIR"/DemoHub_*.zip 2>/dev/null | head -1)

if [ -z "$ZIP" ]; then
  echo "错误：在 $DOWNLOAD_DIR 中未找到 DemoHub_*.zip"
  echo "请确认 ZIP 文件已保存到手机 Download 目录"
  exit 1
fi

echo "  找到：$ZIP"

echo ""
echo "[2/4] 解压中..."
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
unzip -q "$ZIP" -d "$WORK_DIR"

REPO_DIR=$(find "$WORK_DIR" -maxdepth 2 -name ".git" -type d 2>/dev/null | head -1 | xargs -I{} dirname {})

if [ -z "$REPO_DIR" ]; then
  echo "错误：ZIP 中未找到 .git 目录，无法推送"
  echo "请确认 ZIP 由 package_demo.sh 生成（含完整 .git 历史）"
  rm -rf "$WORK_DIR"
  exit 1
fi

echo "  仓库目录：$REPO_DIR"

echo ""
echo "[3/4] 配置 Gitee Remote..."
cd "$REPO_DIR"
git remote remove gitee 2>/dev/null || true
git remote add gitee "$GITEE_URL"
echo "  Remote 已设置：$GITEE_URL"

echo ""
echo "[4/4] 推送 $BRANCH 到 Gitee..."
git push gitee "$BRANCH"

rm -rf "$WORK_DIR"

echo ""
echo "========================================"
echo "  推送完成！"
echo "========================================"
