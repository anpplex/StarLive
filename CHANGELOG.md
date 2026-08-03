# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/) 精神，版本号语义化。

## [Unreleased]

### Planned

- Lyra 侧 ContentProvider 一键导入
- 在线主题商店（非兑换码路径）

## [0.1.5-redeem] — 2026-08-03

### Added

- 主题包兑换：首页「兑换主题」→ 输入码 → LicenseHub 核销 → 下载 zip → 入库本地图库
- `DeviceIdentity`（一码一设备 device_id）
- `RedeemClient` + `ThemePackInstaller`（catalog.json + 壁纸文件）
- `BuildConfig.REDEEM_API_BASE`（默认 `https://buy.998618.xyz`，可 `-PREDEEM_API_BASE=` 覆盖）
- 安全文档 `docs/SECURITY-REDEEM-CODES.md`
- 主题包格式 `docs/THEME-PACK.md` · 打包脚本 `scripts/make-theme-pack.sh`

## [0.1.4-lib] — 2026-08-03

### Added

- 本地导入多图库（最多 24，长按删除）
- ContentProvider `content://com.starlive.app.handoff/active|meta` 供 Lyra 后续对接

## [0.1.3-demo] — 2026-08-03

### Changed

- 示范壁纸改为宽幅影像级素材（简约 / 氛围 / 抽象 × 深浅）
- 补充 `docs/patches/lyra-import-starlive-handoff.md`（Lyra 导入路径说明）


### Added

- 三套示范主题深/浅双图（切换日夜自动换图）
- 导出 handoff 同时写入 `Download/` 根目录（Lyra 默认可扫）
- Lyra：Download 导入兼容 `starlive_wallpaper` 与 `Download/StarLive/`

## [0.1.1-p05] — 2026-08-03

### Added

- 示范轮播与 1–60 分钟间隔
- 已装 Lyra 时自动让路（可关）
- 关于页：通知使用权、电池优化入口

## [0.1.0-p0] — 2026-08-03

### Added

- P0 MVP：星环上屏、4 demo、导入/裁切、空闲开关、日夜、FGS、播歌让出、开机恢复
- 定制 / 规格 / 关于 / 升级 Lyra（含 handoff 导出到 Download/StarLive）
- 产品文档 bootstrap、Apache-2.0、GitHub Flow
