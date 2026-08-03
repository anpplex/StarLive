# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/) 精神，版本号语义化。

## [Unreleased]

### Planned

- 在线主题商店（非兑换码路径）
- Lyra 侧可选依赖 `ring-wallpaper-core`（替代拷贝常量）
- 实车矩阵逐项勾选归档

## [0.1.9-car] — 2026-08-03

### Fixed

- MainActivity CTA `LayoutParams` NPE（车机冷启动崩溃）
- 主页包 `ScrollView`，车机横屏可滚到「已装 Lyra 时让路」等开关

### Added

- `scripts/install-starlive-car.sh`（华为车机 installer 旁路，与 Lyra 同套路）

### Verified on device

- 车型 ICHU3200E15-ADV · serial LD249H019625
- 应用上屏：`cluster_panel` displayId=1 · 4032×284 · 左表盘 1042 共存

## [0.1.8-polish] — 2026-08-03

### Added

- 关于页「检查更新」（GitHub Releases，可选联网）
- `scripts/ops-upload-pack.sh` 上传主题包到 LicenseHub VPS

### Changed

- handoff.json 用 JSONObject 写出，并写入 contentProvider URI
- 定制页说明兑换码交付方式
- 隐私：补充「检查更新」联网说明

### Docs (earlier unreleased)

- `docs/QA-MATRIX.md` · `docs/CUSTOM-SOP.md` · `docs/INSTALL.md`
- GitHub Actions assembleDebug · `scripts/install-starlive.sh`

## [0.1.7-core] — 2026-08-03

### Added

- Android 库模块 `:ring-wallpaper-core`（`com.starlive.ring`）：
  `StripGeometry` · `WallpaperCropper` · `WallpaperEdgeSoftener`
- 文档 `docs/RING-WALLPAPER-CORE.md`

### Changed

- App 依赖 core 模块，删除 app 内重复几何/裁切实现

## [0.1.6-redeem] — 2026-08-03

### Changed

- 兑换成功后可「应用上屏」（含播歌让出待生效）
- 隐私/关于：兑换联网说明（#11）

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
