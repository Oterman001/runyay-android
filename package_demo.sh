#!/usr/bin/env bash
# 打包脚本 - Mac/Linux
# 步骤：clean -> 复制目录 -> 删除 zrun -> 压缩 -> 拷贝到 assets

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
PROJECT_NAME="DemoHub"
DATE=$(date +"%Y%m%d")
COPY_NAME="${PROJECT_NAME}_${DATE}"
PARENT_DIR="$(dirname "$PROJECT_DIR")"
COPY_DIR="${PARENT_DIR}/${COPY_NAME}"
ZIP_FILE="${PARENT_DIR}/${COPY_NAME}.zip"
ASSETS_DIR="${PROJECT_DIR}/rundemo/src/main/assets"

echo "========================================"
echo "  DemoHub 打包脚本"
echo "  日期: $DATE"
echo "========================================"

# 步骤 1：执行 Gradle clean
echo ""
echo "[1/5] 执行 Gradle clean..."
cd "$PROJECT_DIR"
./gradlew :app:clean :fitdemo:clean :rundemo:clean :m3demo:clean
echo "  clean 完成"

# 步骤 2：复制整个 DemoHub 目录
echo ""
echo "[2/5] 复制目录 -> ${COPY_DIR}"
if [ -d "$COPY_DIR" ]; then
    echo "  目标目录已存在，先删除旧的..."
    rm -rf "$COPY_DIR"
fi
cp -r "$PROJECT_DIR" "$COPY_DIR"
echo "  复制完成"

# 步骤 3：删除副本中的 zrun 目录
echo ""
echo "[3/5] 删除副本中的 zrun 目录..."
if [ -d "${COPY_DIR}/zrun" ]; then
    rm -rf "${COPY_DIR}/zrun"
    echo "  zrun 目录已删除"
else
    echo "  zrun 目录不存在，跳过"
fi

# 步骤 4：压缩打包
echo ""
echo "[4/5] 压缩为 ${COPY_NAME}.zip..."
if [ -f "$ZIP_FILE" ]; then
    echo "  压缩包已存在，先删除旧的..."
    rm -f "$ZIP_FILE"
fi
cd "$PARENT_DIR"
zip -r "${COPY_NAME}.zip" "${COPY_NAME}/"
echo "  压缩完成: $ZIP_FILE"

# 步骤 5：拷贝压缩包到 rundemo assets
echo ""
echo "[5/5] 拷贝压缩包到 rundemo/src/main/assets/..."
mkdir -p "$ASSETS_DIR"
cp "$ZIP_FILE" "$ASSETS_DIR/"
echo "  拷贝完成: ${ASSETS_DIR}/${COPY_NAME}.zip"

echo ""
echo "========================================"
echo "  打包完成！"
echo "  压缩包: $ZIP_FILE"
echo "  assets: ${ASSETS_DIR}/${COPY_NAME}.zip"
echo "========================================"
