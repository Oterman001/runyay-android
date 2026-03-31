"""
配置加载器：从 config.yaml 读取并用 pydantic 校验。
"""

from __future__ import annotations

from pathlib import Path
from typing import Tuple

import yaml
from pydantic import BaseModel, Field, field_validator


class DelayConfig(BaseModel):
    mu: float
    sigma: float


class DelaysConfig(BaseModel):
    tap: DelayConfig = DelayConfig(mu=0.8, sigma=0.3)
    between_profiles: DelayConfig = DelayConfig(mu=3.0, sigma=1.2)
    post_follow: DelayConfig = DelayConfig(mu=10.0, sigma=4.0)
    long_break_probability: float = 0.15
    long_break_extra_min: float = 20.0
    long_break_extra_max: float = 60.0
    scroll_pause: DelayConfig = DelayConfig(mu=1.5, sigma=0.6)


class DeviceConfig(BaseModel):
    serial: str = ""


class AppConfig(BaseModel):
    device: DeviceConfig = DeviceConfig()
    dry_run: bool = True
    daily_follow_limit: int = Field(40, ge=1, le=200)
    max_follows_per_session: int = Field(15, ge=1)
    session_break_seconds: int = Field(180, ge=0)
    active_hours: Tuple[int, int] = (9, 22)
    min_score_threshold: int = Field(60, ge=0, le=100)
    random_skip_ratio: float = Field(0.12, ge=0.0, le=1.0)
    delays: DelaysConfig = DelaysConfig()
    # 推荐列表路径（v2）
    mode: str = Field("auto", pattern="^(auto|recommend|my_following)$")
    pages_per_recommend: int = Field(8, ge=1)
    max_depth: int = Field(5, ge=1, le=10)
    direct_follow: bool = True  # True=直接点列表关注按钮，False=进主页评分后关注
    # 博主关注列表探索（仅 direct_follow=False 时有意义）
    explore_following_list: bool = False
    max_following_list_users: int = Field(8, ge=1)
    # 我的关注列表探索模式（mode=my_following）专用
    max_bloggers_to_explore: int = Field(10, ge=1)  # 从我的关注列表取多少人作为探索入口

    @field_validator("active_hours", mode="before")
    @classmethod
    def validate_hours(cls, v):
        if isinstance(v, list) and len(v) == 2:
            return tuple(v)
        return v


def load_config(path: str = "config.yaml") -> AppConfig:
    config_path = Path(path)
    if not config_path.exists():
        # 项目根目录相对路径
        config_path = Path(__file__).parent.parent / "config.yaml"

    with open(config_path, encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    return AppConfig(**raw)
