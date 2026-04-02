# -*- coding: utf-8 -*-
"""
软件著作权 — 源代码 PDF 生成脚本
规范要求：
  - PDF 格式，前 30 页 + 后 30 页，共 60 页
  - 每页 >= 50 行，标明页码
  - 第 1 页为模块开头，第 60 页为模块结尾
  - 编程语言：Kotlin
"""

import os
import sys
import io

# 强制 stdout 使用 UTF-8（解决 Windows GBK 终端乱码）
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# ── 中文字体注册 ──────────────────────────────────────────────────────────────

CJK_FONT = None

def _try_register(font_name, path):
    global CJK_FONT
    if CJK_FONT:
        return
    if os.path.exists(path):
        try:
            pdfmetrics.registerFont(TTFont(font_name, path))
            CJK_FONT = font_name
            print(f"[INFO] 已注册中文字体: {font_name} ({path})")
        except Exception as e:
            print(f"[WARN] 注册字体失败 {path}: {e}")

_try_register("SimHei",   r"C:\Windows\Fonts\simhei.ttf")
_try_register("MSYaHei",  r"C:\Windows\Fonts\msyh.ttc")
_try_register("SimSun",   r"C:\Windows\Fonts\simsun.ttc")

if not CJK_FONT:
    print("[WARN] 未找到可用中文字体，中文字符将显示为方块")


def has_cjk(text):
    """判断字符串是否包含 CJK 字符"""
    for ch in text:
        cp = ord(ch)
        if (0x4E00 <= cp <= 0x9FFF   # 基本汉字
                or 0x3400 <= cp <= 0x4DBF   # 扩展A
                or 0x20000 <= cp <= 0x2A6DF  # 扩展B
                or 0x3000 <= cp <= 0x303F    # CJK 标点
                or 0xFF00 <= cp <= 0xFFEF):  # 全角字符
            return True
    return False


# ── 配置 ─────────────────────────────────────────────────────────────────────

SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
BASE = os.path.join(
    PROJECT_ROOT,
    "rundemo", "src", "main", "java", "com", "oterman", "rundemo"
)

OUTPUT_PDF = os.path.join(SCRIPT_DIR, "\u6e90\u4ee3\u7801.pdf")

# 页面布局
PAGE_WIDTH, PAGE_HEIGHT = A4
MARGIN_LEFT   = 20 * mm
MARGIN_RIGHT  = 20 * mm
MARGIN_TOP    = 20 * mm
MARGIN_BOTTOM = 18 * mm

# 字体
FONT_CODE   = "Courier"      # ASCII 代码行
FONT_CJK    = CJK_FONT or FONT_CODE  # 含中文行
FONT_SIZE   = 7.5
LINE_HEIGHT = 9.5
HEADER_FONT_SIZE = 8
FOOTER_FONT_SIZE = 8

# 每页内容区
CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM - 14 * mm
LINES_PER_PAGE = int(CONTENT_HEIGHT / LINE_HEIGHT)   # 约 70 行

# 软件信息
SOFTWARE_NAME  = "\u8dd1\u6b65\u8fd0\u52a8\u52a9\u624b"   # 跑步运动助手
COPYRIGHT_TEXT = "Copyright (C) 2026 Oterman. All Rights Reserved."

# ── 选取的源文件（按功能模块顺序，共 25 个）────────────────────────────────

SOURCE_FILES = [
    # --- 导航与路由 ---
    os.path.join("presentation", "navigation", "Screen.kt"),
    os.path.join("presentation", "navigation", "NavGraph.kt"),
    # --- 用户认证 ---
    os.path.join("data", "network", "api", "UserApi.kt"),
    os.path.join("data", "repository", "UserRepository.kt"),
    os.path.join("presentation", "feature", "auth", "login", "LoginScreen.kt"),
    os.path.join("presentation", "feature", "auth", "login", "LoginViewModel.kt"),
    os.path.join("presentation", "feature", "auth", "register", "RegisterViewModel.kt"),
    os.path.join("presentation", "feature", "auth", "register", "RegisterScreen.kt"),
    # --- 新用户引导 ---
    os.path.join("presentation", "feature", "onboarding", "physio", "PhysioSetupScreen.kt"),
    # --- 主页 ---
    os.path.join("presentation", "feature", "home", "HomeViewModel.kt"),
    os.path.join("presentation", "feature", "home", "HomeScreen.kt"),
    os.path.join("presentation", "feature", "home", "tabs", "DataTab.kt"),
    os.path.join("presentation", "feature", "home", "tabs", "DashboardTabViewModel.kt"),
    # --- 跑步数据 ---
    os.path.join("data", "network", "api", "RunDataApi.kt"),
    os.path.join("data", "repository", "RunDataRepositoryImpl.kt"),
    # --- 跑步记录详情 ---
    os.path.join("presentation", "feature", "rundetail", "RunDetailViewModel.kt"),
    os.path.join("presentation", "feature", "rundetail", "RunDetailScreen.kt"),
    # --- 数据源管理 ---
    os.path.join("data", "repository", "DataSourceRepository.kt"),
    os.path.join("presentation", "feature", "datasource", "DataSourceDetailViewModel.kt"),
    # --- 跑鞋管理 ---
    os.path.join("data", "repository", "RunningShoeRepository.kt"),
    os.path.join("presentation", "feature", "runningshoes", "detail", "RunningShoeDetailScreen.kt"),
    # --- 运动分享 ---
    os.path.join("presentation", "feature", "share", "ShareViewModel.kt"),
    # --- 心率区间设置 ---
    os.path.join("presentation", "feature", "settings", "heartrate", "HearRateZoneScreen.kt"),
    # --- 个人资料 ---
    os.path.join("presentation", "feature", "userprofile", "UserProfileViewModel.kt"),
    os.path.join("presentation", "feature", "userprofile", "UserProfileScreen.kt"),
]


# ── 工具函数 ──────────────────────────────────────────────────────────────────

def load_all_lines():
    """读取所有源文件，返回 [(display_line_text, filename_label)] 列表"""
    all_lines = []
    for rel_path in SOURCE_FILES:
        full_path = os.path.join(BASE, rel_path)
        label = rel_path.replace(os.sep, "/")
        if not os.path.exists(full_path):
            print(f"[WARN] File not found, skipped: {full_path}")
            continue
        with open(full_path, "r", encoding="utf-8", errors="replace") as f:
            file_lines = f.readlines()
        # 使用纯 ASCII 分隔符，避免非等宽字体问题
        all_lines.append(("// ============================================================", label))
        all_lines.append((f"// File: {label}", label))
        all_lines.append(("// ============================================================", label))
        for raw in file_lines:
            all_lines.append((raw.rstrip("\n"), label))
    return all_lines


def paginate(all_lines, lines_per_page):
    pages = []
    for i in range(0, len(all_lines), lines_per_page):
        pages.append(all_lines[i: i + lines_per_page])
    return pages


def get_page_header_label(page_lines):
    label = ""
    for _, lbl in page_lines:
        if lbl:
            label = lbl
    return label


def clean_line(text):
    """清理控制字符（保留 tab）"""
    result = []
    for ch in text:
        code = ord(ch)
        if code < 32 and code != 9:
            result.append(" ")
        else:
            result.append(ch)
    return "".join(result)


# ── PDF 渲染 ──────────────────────────────────────────────────────────────────

def draw_string_auto(c, x, y, text, font_ascii, font_cjk, size):
    """根据文本内容自动选择字体渲染（含 CJK 用 CJK 字体，否则用等宽字体）"""
    if has_cjk(text) and font_cjk != font_ascii:
        c.setFont(font_cjk, size)
    else:
        c.setFont(font_ascii, size)
    try:
        c.drawString(x, y, text)
    except Exception:
        # 兜底：将无法渲染的字符替换为 ?
        safe = "".join(ch if ord(ch) < 128 else "?" for ch in text)
        c.setFont(font_ascii, size)
        c.drawString(x, y, safe)


def draw_page(c, page_lines, page_number, total_pages):
    """在 canvas c 上绘制一页内容"""
    # ---- 页眉 ----
    header_y = PAGE_HEIGHT - MARGIN_TOP + 3 * mm
    c.setFillColorRGB(0.35, 0.35, 0.35)
    # 软件名（中文，用 CJK 字体）
    draw_string_auto(c, MARGIN_LEFT, header_y, SOFTWARE_NAME,
                     FONT_CJK, FONT_CJK, HEADER_FONT_SIZE)
    # 右侧文件名（ASCII）
    file_label = get_page_header_label(page_lines)
    short_label = file_label.split("/")[-1] if file_label else ""
    c.setFont(FONT_CODE, HEADER_FONT_SIZE)
    c.drawRightString(PAGE_WIDTH - MARGIN_RIGHT, header_y, short_label)

    # 页眉分隔线
    c.setStrokeColorRGB(0.7, 0.7, 0.7)
    c.setLineWidth(0.4)
    sep_y = header_y - 2.5 * mm
    c.line(MARGIN_LEFT, sep_y, PAGE_WIDTH - MARGIN_RIGHT, sep_y)

    # ---- 代码内容 ----
    c.setFillColorRGB(0, 0, 0)
    content_top_y = PAGE_HEIGHT - MARGIN_TOP - 8 * mm
    max_text_width = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT - 4 * mm

    for idx, (line_text, _) in enumerate(page_lines):
        y = content_top_y - idx * LINE_HEIGHT
        raw = clean_line(line_text).replace("\t", "    ")
        display_text = f"{idx + 1:3d}  {raw}"

        # 截断超宽行（按字符估算，避免 stringWidth 频繁调用）
        font_for_measure = FONT_CJK if has_cjk(display_text) else FONT_CODE
        while len(display_text) > 6:
            w = c.stringWidth(display_text, font_for_measure, FONT_SIZE)
            if w <= max_text_width:
                break
            display_text = display_text[:-1]

        c.setFillColorRGB(0, 0, 0)
        draw_string_auto(c, MARGIN_LEFT, y, display_text,
                         FONT_CODE, FONT_CJK, FONT_SIZE)

    # ---- 页脚分隔线 ----
    footer_line_y = MARGIN_BOTTOM + 7 * mm
    c.setStrokeColorRGB(0.7, 0.7, 0.7)
    c.line(MARGIN_LEFT, footer_line_y, PAGE_WIDTH - MARGIN_RIGHT, footer_line_y)

    # ---- 页码 ----
    c.setFont(FONT_CODE, FOOTER_FONT_SIZE)
    c.setFillColorRGB(0.3, 0.3, 0.3)
    c.drawCentredString(PAGE_WIDTH / 2, MARGIN_BOTTOM + 2.5 * mm, f"- {page_number} -")

    # ---- 版权（纯 ASCII）----
    c.setFont(FONT_CODE, 6)
    c.setFillColorRGB(0.6, 0.6, 0.6)
    c.drawRightString(PAGE_WIDTH - MARGIN_RIGHT, MARGIN_BOTTOM + 2.5 * mm, COPYRIGHT_TEXT)


def generate_pdf(selected_pages, output_path):
    c = canvas.Canvas(output_path, pagesize=A4)
    c.setTitle("Source Code - Running Assistant")
    c.setAuthor("Oterman")
    c.setSubject("Software Copyright Source Code")

    for pdf_page_number, (_, page_lines) in enumerate(selected_pages, start=1):
        draw_page(c, page_lines, pdf_page_number, len(selected_pages))
        c.showPage()

    c.save()
    print(f"[OK] PDF generated: {output_path}  ({len(selected_pages)} pages)")


# ── 主流程 ────────────────────────────────────────────────────────────────────

def main():
    print(f"[INFO] Source root: {BASE}")
    print(f"[INFO] Lines per page: {LINES_PER_PAGE}")

    all_lines = load_all_lines()
    print(f"[INFO] Total lines: {len(all_lines)}")

    pages = paginate(all_lines, LINES_PER_PAGE)
    total = len(pages)
    print(f"[INFO] Total pages: {total}")

    if total < 60:
        print("[INFO] Less than 60 pages, outputting all")
        selected = [(i + 1, p) for i, p in enumerate(pages)]
    else:
        front = pages[:30]
        back  = pages[total - 30:]
        selected = (
            [(i + 1, p) for i, p in enumerate(front)]
            + [(total - 30 + i + 1, p) for i, p in enumerate(back)]
        )
        print(f"[INFO] Front 30 (pages 1-30) + Back 30 (pages {total-29}-{total}) = 60 pages")

    generate_pdf(selected, OUTPUT_PDF)

    print("\n-- Verification --")
    print(f"  Output : {OUTPUT_PDF}")
    print(f"  Pages  : {len(selected)}")
    print(f"  Lines/page : {LINES_PER_PAGE} (>= 50 OK)")
    first_file = pages[0][0][1] if pages else ""
    last_file  = pages[-1][-1][1] if pages else ""
    print(f"  First page file : {first_file.split('/')[-1] if first_file else '-'}")
    print(f"  Last page file  : {last_file.split('/')[-1] if last_file else '-'}")


if __name__ == "__main__":
    main()
