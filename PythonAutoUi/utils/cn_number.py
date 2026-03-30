"""
中文数字解析：将小红书显示的 "1.2万"、"3.5亿" 等转换为整数。
"""

import re


def parse_cn_number(text: str) -> int:
    """
    解析中文数字格式：
        "1.2万"  → 12000
        "3.5亿"  → 350000000
        "892"    → 892
        "1,234"  → 1234
        ""       → 0
    """
    if not text:
        return 0

    text = text.strip().replace(",", "").replace(" ", "")

    # 提取数字部分（支持小数）
    match = re.search(r"[\d.]+", text)
    if not match:
        return 0

    num = float(match.group())

    if "亿" in text:
        return int(num * 100_000_000)
    elif "万" in text:
        return int(num * 10_000)
    else:
        return int(num)
