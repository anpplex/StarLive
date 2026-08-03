# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/) 精神，版本号语义化。

## [Unreleased]

### Planned

- 在线主题商店（非兑换码路径）
- QA 剩余 ⬜：I1 第三方文件管理器内完整点选（选择器已通）；R3/R4 Admin

## [0.1.32-polish] — 2026-08-04

### Product / docs

- 统一「更多」与各功能页文案（显示设置、兑换、定制、规格、升级、关于）
- 删除 Agent / 流程类文档，对外文档去口语化
- 状态用语统一：显示中 / 未显示 / Lyra 优先 等

## [0.1.31-ambient] — 2026-08-04

### Ambient / remote strip

- 星环全条铺日夜 base_map 玻璃，修左侧透明黑底与系统浅色/深色过渡不同步
- 自适应日夜：`Configuration` + UiModeManager 硬 YES/NO + ContentObserver；自适应轮询 0.8s
- 翻转时 `invalidateStripCache` 强制重烤羽化边

## [0.1.30-bidir] — 2026-08-03

### UX / Gallery

- 横滑改为**自绘拖拽 + 手指位移判定方向**，左右均可：`1↔2↔3↔4` 循环
- 每次停稳**固定停在中间段**，避免贴边后只能单向滑
- 仍保持每次手势最多 ±1 页 + 阻尼吸附

## [0.1.29-snap] — 2026-08-03

### UX / Gallery

- 壁纸横滑：**每次手势最多 ±1 页**（按手势起点算，不再按中点自由飞）
- 关掉 HorizontalScrollView 自由 fling；松手阻尼 ease-out 吸附（280–480ms）
- 修「滑一下跳到第 3 张」

## [0.1.28-copy] — 2026-08-03

### UX / Copy

- 术语统一：应用到星环 · Lyra 优先 · 内置轮播 · 显示设置
- 去掉 Display/cluster/API/包名等开发者用语；失败提示用户可读
- 首页去掉版本串与滑页 Toast；页码简化为 `n / N`；成功态副文案可隐藏
- 关于页收敛为版本/权限/声明；规格页只谈尺寸与裁切

## [0.1.27-gallery] — 2026-08-03

### UX

- **顶部预览即壁纸库**：左右滑动切换示范+导入图（全幅条带）
- 去掉下方示范/库分区；**导入**在右上角「更多」左侧
- 应用上屏紧贴预览；长按导入页可删除

## [0.1.26-home] — 2026-08-03

### UX / IA

- 首页 HMI 重排；更多菜单；预览全幅壁纸带

## [0.1.25-demo] — 2026-08-03

### UX

- 内置示范 4 张替换为：`Beauty` · `SnowKing` · `Sanrio` · `Metro`（2990×284）
- 日/夜共用同一张图；旧 id 简约/氛围/抽象迁移到 Beauty

## [0.1.24-cache] — 2026-08-03

### Perf / Import

- `decodeActiveForStrip` 按 path·mtime·night 缓存，避免首页/星环重复羽化
- 日夜 ambient 刷新时重建「自动·浅/深」chip 与示范轨
- 导入优先 **SAF OpenDocument**（车机多用户更稳），失败回落 GetContent

### Verified on device (LD249H019625)

- **M1** `com.huawei.music.auto` 播歌 → 胶囊让出 · 无 ClusterStrip
- **M1b** Wallpaper Engine 不挡上屏（0.1.22）
- **M2** MEDIA_PAUSE ≥8s → `after-play` 夺回上屏
- **M3** MEDIA_NEXT 切歌空隙：`grace=3000ms` · 无闪上屏；曲目切换正常
- **A2** `ui_night_mode` 翻转 ambient
- **I2** Download 导入确认入库

## [0.1.23-preview] — 2026-08-03

### UX

- 首页预览与星环同源：`decodeActiveForStrip`（左缘羽化 + 日夜玻璃）
- 切回示范时清除残留 `custom_label`（避免「夜色」脏标签）

## [0.1.22-music] — 2026-08-03

### Fixed

- **假播歌让出**：Wallpaper Engine / motif 等壁纸 MediaPlayer 使 `isMusicActive` 常 true
- `MusicPlaybackFilter`：按 usage + 包名黑名单过滤；`MediaProbe` 优先读 `activePlaybackConfigurations`
- NLS MediaSession 同样忽略壁纸包

## [0.1.21-ambient] — 2026-08-03

### UX / 远端

- **远端日夜自动跟随**（对齐 Lyra）：`ui_night_mode` 浅色1 / 深色2 / 自适应9 + Configuration + 22:00–06:00 时钟兜底
- 星环壁纸 **左缘羽化按深浅重烘焙**（玻璃 #E8EAEE / #080A0B）；ambient 翻转 320ms 交叉淡入
- 日夜 chip「自动·浅色/深色」显示当前有效；示范双图随远端日夜切换
- `AmbientWatch` 2s 轮询 + Secure 观察（车机设置改显示模式也能跟上）

## [0.1.20-ui] — 2026-08-03

### UX

- **二级页对齐 UiKit**：关于 / 升级到 Lyra / 规格说明 / 私人定制
- 统一色板、卡片、主/次/幽灵按钮与首页视觉语言一致
- 关于页版本号、检查更新、导航入口；升级页 Lyra 检测与 handoff 导出保留

### Verified on device (LD249H019625)

- 装机 0.1.20-ui · 启动关于/规格/定制/升级页 冒烟（见 QA-MATRIX）

## [0.1.19-qa] — 2026-08-03

### Fixed

- 兑换网络失败统一中文：「网络不可用 · 请检查车机联网后重试」（DNS/超时/连接失败）

### Verified on device (LD249H019625)

- **R6** 指向不可达 API `http://192.0.2.1:9` → 状态栏明确断网文案 ✅  
- **I1** 导入→从相册/文件选择 → 系统「使用以下方式打开」（ES 文件浏览器）✅；完整选文件需人工  

## [0.1.18-ui] — 2026-08-03

### UX

- `UiTokens` / `UiKit`：统一色板、圆角卡片、主/次按钮、chip、设置行（车机大触控）
- **首页重排**：状态胶囊 + 英雄预览卡 → 主操作 → 选择壁纸（示范/导入）→ 显示与恢复 → 日夜 → 页脚
- 示范/图库 chip 显示选中态；空库引导文案
- 兑换页 / 导入确认页对齐同一视觉语言；兑换成功区分「已绑定重下」


### Known limitations

- **冷启开机自启无法持久化（无厂商 UI 白名单）**  
  多次 `adb reboot` 仍无 `boot_probe.log` → BOOT 未投递。  
  **0.1.13+ 补偿**：进程启动自动 recover；**0.1.14** process-start 首跳约 0.4s。  
  见 `docs/QA-MATRIX.md` §B1。
- 车机 **NLS 可能被 iaware 拦截**（`Service starting has been prevented`）；播歌让出依赖 `AudioManager.isMusicActive` 兜底。

## [0.1.17-car] — 2026-08-03

### Fixed / Added

- handoff 导出：多路径写 Download（当前用户 `Environment` + 兼容 emulated/0），减轻多用户车机 Lyra 扫不到
- Main 恢复时再拉 KeepAlive（部分 HU 拦 Application 起 FGS）
- CI：Lyra 核心代码边界 grep（防误拷特效/License）

### Verified on device (LD249H019625 · 0.1.16→0.1.17)

- **C2** 空闲关 → KeepAlive 停、Cluster 释放 ✅  
- **R2** `K4CAR4BH` 再兑 →「已安装…」幂等成功 ✅  
- **L2** handoff meta 可读 · version 0.1.16-polish ✅  
- **B4** 播歌中 process-start 正确跳过（胶囊「播歌中」）；空闲开 + 未播歌路径需 process-start + 应用  

## [0.1.16-polish] — 2026-08-03

### Added / Changed

- `BootRecoverDelays` · `ClusterApplyMessages` · `LibraryIndexCodec`（纯逻辑 + 单测）
- `UpdateChecker.isNewerThan` 单测
- 删除当前库图：自动切下一张或恢复示范（避免 active 悬空）
- README 同步到当前版本与构建/测试命令

## [0.1.15-quality] — 2026-08-03

### Added

- `RedeemExchangeParser` + 单测（R2 幂等 JSON、R4/R3/R6 文案映射）
- `PlaybackGate` 可注入时钟/调度 + M3 切歌空隙单测（&lt;3s 不让出、暂停 8s 让出）
- Release 签名：`keystore.properties.example` · `scripts/build-release.sh` · `scripts/publish-github-release.sh`
- `scripts/test-redeem-api.sh` 兑换 API 冒烟（无需车机）
- CI：`:app:testDebugUnitTest`

### Notes

- Lyra 仓：依赖本地 `StarLive/android/ring-wallpaper-core` + 几何契约单测（另提 PR）

## [0.1.14-car] — 2026-08-03

### Added

- `CropStrategy` 纯尺寸策略 + JUnit（I3 EXACT / I4 BAND / CENTER / 非法 bounds）
- CI：`:ring-wallpaper-core:testDebugUnitTest`
- process-start recover 首跳 **400ms**（BOOT 仍用 2.5/8/20s）

### Verified on device (LD249H019625)

- **I4** 2990×284 → 确认页「尺寸匹配」→ 应用 ✅  
- **I3** 4032×284 → 「右带 / 跳过表盘」→ 应用 ✅  
- **I5** 损坏图 → 无法读取、回主页、不崩 ✅  

## [0.1.13-car] — 2026-08-03

### Added

- 进程启动恢复：`StarLiveApp` 在空闲开且有图时 `bootScheduler.schedule("process-start")`，弥补车机不投递 BOOT
- 上屏状态同步：`StripOrchestrator` UI 刷新广播 + 首页延迟/实时刷新，避免卡在「未上屏」
- 关于页 / 空闲副文案：冷启限制与「打开 App 一次即可恢复」说明

### Verified on device (LD249H019625)

- **B4** force-stop → 打开 App → ~4s 内 ClusterStrip + KeepAlive ✅  
- **C5** 日夜 深/浅/自动 ✅ · **I6** 图库「夜色」切换 ✅ · **I7** 恢复示范 → minimal ✅  
- 首页胶囊「已上屏」与 cluster 一致 ✅  
- **L2** `content://com.starlive.app.handoff/meta|active` 可读 ✅  

## [0.1.12-car] — 2026-08-03

### Added

- `boot_probe.log`：`StartupReceiver` 被调用时落盘（冷启 logcat 易丢）

### Verified on device

- **B1 开机自启 ⚠️ 已知限制**（reboot ≥4）：进程未起、无 KeepAlive、无 `boot_probe.log` → 系统未把 BOOT 交给星澜（非 App 逻辑未调度）；adb `deviceidle` 白名单无效

## [0.1.11-car] — 2026-08-03

### Fixed

- 播歌让出：车机 NLS 常不回调时，`KeepAliveService` 用 `AudioManager.isMusicActive` 轮询兜底

### Verified on device

- 空闲关 → 让出原厂；空闲开 → 再上屏
- 网易云播放中应用 → deferred + 播歌让出；暂停 ~8s → 自动夺回

## [0.1.10-car] — 2026-08-03

### Fixed

- Download 导入：多用户车机扫描 `Environment` 公共目录、`filesDir`、MediaStore；导入前申请读图权限

### Verified on device (LD249H019625)

- 示范切换 / 应用上屏
- 导入确认 → 应用上屏 (cluster display 1)
- 「已装 Lyra 时让路」ON → `lyra handoff` + release 星环

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
