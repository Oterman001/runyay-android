#!/usr/bin/env bash
# 应用 patch 文件到当前 git 仓库
# 使用方式：
#   从项目根目录：bash apply_patches.sh
#   从 zip 解压目录：bash apply_patches.sh
# 脚本会自动查找与自身同目录下的所有 *.patch 文件
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  Patch 应用脚本"
echo "========================================"

# ── 检查是否在 git 仓库内 ─────────────────
if ! git rev-parse --git-dir &>/dev/null; then
  echo "错误：当前目录不在 git 仓库中。"
  echo "请在目标仓库根目录运行本脚本，或将本脚本复制到仓库根目录后运行。"
  exit 1
fi

# ── 查找 patch 文件 ───────────────────────
echo ""
echo "[1/2] 查找 patch 文件（目录：$SCRIPT_DIR）..."
mapfile -t PATCHES < <(find "$SCRIPT_DIR" -maxdepth 1 -name "*.patch" | sort)

if [ ${#PATCHES[@]} -eq 0 ]; then
  echo "错误：在 $SCRIPT_DIR 中未找到任何 *.patch 文件。"
  echo "请确认已将 patch 文件放在与本脚本相同的目录下。"
  exit 1
fi

echo "  找到 ${#PATCHES[@]} 个 patch 文件："
for p in "${PATCHES[@]}"; do
  echo "    $(basename "$p")"
done

# ── 应用 patch ────────────────────────────
echo ""
echo "[2/2] 应用 patch（使用 git am --3way）..."
echo ""

if git am --3way "${PATCHES[@]}"; then
  echo ""
  echo "========================================"
  echo "  完成！已成功应用 ${#PATCHES[@]} 个 patch。"
  echo "  提交历史（最新 ${#PATCHES[@]} 条）："
  git log --oneline -"${#PATCHES[@]}"
  echo "========================================"
else
  echo ""
  echo "========================================"
  echo "  错误：patch 应用过程中遇到冲突！"
  echo ""
  echo "  选项 A — 手动解决冲突后继续："
  echo "    1. 编辑冲突文件"
  echo "    2. git add <冲突文件>"
  echo "    3. git am --continue"
  echo ""
  echo "  选项 B — 跳过当前 patch："
  echo "    git am --skip"
  echo ""
  echo "  选项 C — 放弃全部应用："
  echo "    git am --abort"
  echo "========================================"
  exit 1
fi
