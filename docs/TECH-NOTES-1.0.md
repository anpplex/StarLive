# 星澜 StarLive 1.0 — 技术说明（TECH）

| 字段 | 值 |
|------|-----|
| 日期 | 2026-08-03 |
| 状态 | P0 开工依据 |
| 产品 | 星澜 / StarLive |
| 上游 | [FEATURE-ALIGNMENT-1.0.md](./FEATURE-ALIGNMENT-1.0.md) · [INTERACTION-1.0.md](./INTERACTION-1.0.md) |
| 参考实现 | `/Users/anpple/Codex/Lyra/android`（**拷贝精简，不链 monorepo 模块**） |

---

## 1. 目标与非目标

### 1.1 目标（P0）

1. 阿维塔星环 **2990×284** 空闲壁纸自绘上屏  
2. 4 张 demo + 导入（选图 / Download）+ 裁切确认  
3. 空闲开关、日夜基础、预览条  
4. **播歌让出** + **开机/解锁恢复（随空闲显示）**  
5. 轻量 FGS、私人定制静态页、关于/规格  
6. **Lyra 子项目地基**：几何/文件名/handoff 契约兼容，支持 **丝滑升级**（见 [LYRA-UPGRADE.md](./LYRA-UPGRADE.md)）  

### 1.2 非目标

| 禁止带入 | 来源 |
|----------|------|
| License / LicenseHub / license-sdk | Lyra |
| 歌词 logcat、特效 WebView、Orchestrator 特效态 | Lyra |
| 在线商城、支付、订阅 | 凯迪 |
| LivePaper / libct5 / .kzb | 凯迪 |
| 508 张 catalog | Lyra assets |
| 破坏 handoff 的私有分辨率/私有仅星澜文件格式 | 升级硬约束 |

### 1.3 与 Lyra 的工程关系

| 项 | 1.0 | 以后 |
|----|-----|------|
| 仓库 | 独立 `anpplex/StarLive` | 可仍独立；core 抽共享 |
| applicationId | `com.starlive.app`（**并排可装**，利升级过渡） | 不变 |
| 代码 | 从 Lyra **拷贝精简**壁纸层 | ✅ StarLive 已建 `:ring-wallpaper-core`；Lyra 可渐进依赖 |
| 数据 | Download 文件名互通 + `starlive-handoff/v1` | ContentProvider 自动迁移 |
| 升级 UX | Footer/About CTA | Lyra 内「从星澜导入」 |

**红线：** 任何壁纸输出必须能被 Lyra `ClusterWallpaperSettings.importFromFile`（或等价 API）消费。

---

## 2. 工程骨架

### 2.1 仓库布局（建议）

```text
/Users/anpple/Codex/StarLive/
├── README.md                 # 后补
├── LICENSE                   # Apache-2.0
├── docs/
│   ├── FEATURE-ALIGNMENT-1.0.md
│   ├── INTERACTION-1.0.md
│   ├── AUDIT-GAPS-1.0.md
│   └── TECH-NOTES-1.0.md     # 本文
└── android/
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle.properties
    └── app/
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/starlive/app/...
            ├── res/
            └── assets/
                ├── wallpaper/          # 4 demo + catalog.json
                └── contact/            # wechat id / qr（可替换）
```

### 2.2 Gradle 基线

| 项 | 建议值 | 依据 |
|----|--------|------|
| `namespace` / `applicationId` | **`com.starlive.app`** | 独立于 `com.lyra.cluster`；可日后改品牌域名 |
| `minSdk` | **28** | 对齐 Lyra 车机 |
| `compileSdk` / `targetSdk` | **35** | 对齐 Lyra |
| Java/Kotlin | **17** | 对齐 Lyra |
| 依赖 | `core-ktx` · `appcompat` · `lifecycle-runtime-ktx` | **不要** license-sdk |
| 版本 | `versionName=0.1.0` · `versionCode=1` | 首版迭代 |

> 若需与 998618 品牌统一，可改为 `xyz.n998618.starlive` 等，**在创建工程时改一次，勿与 Lyra 同 applicationId**。

### 2.3 构建命令（约定）

```bash
cd android && ./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. 模块划分（包结构）

```text
com.starlive.app
├── StarLiveApp.kt                 # Application：seed、可选起服务
├── display/
│   ├── StripGeometry.kt           # 4032/1042/2990/284 + feather 常量
│   ├── ClusterDisplayController.kt
│   ├── SoftLeftFadeImageView.kt
│   ├── WallpaperEdgeSoftener.kt
│   ├── WallpaperEdgeDrawable.kt   # 若 softener 依赖
│   └── OemClusterColors.kt        # 日夜 junction 色，可精简
├── wallpaper/
│   ├── WallpaperRepository.kt     # 替代 ClusterWallpaperSettings（精简）
│   ├── WallpaperCropper.kt        # 裁切策略（交互定稿）
│   └── DemoCatalog.kt             # catalog.json 加载
├── runtime/
│   ├── StripOrchestrator.kt       # 上屏/让出/恢复；非 Lyra 特效编排
│   ├── PlaybackGate.kt            # isPlaying + 8s/3s 宽限
│   ├── PendingApplyStore.kt       # 持久化 pending
│   └── BootRecoverScheduler.kt    # 2.5/8/20s
├── service/
│   ├── KeepAliveService.kt        # 轻量 FGS
│   ├── MediaSessionProbe.kt       # 或 Listener 内聚
│   ├── StarLiveNotificationListener.kt
│   └── StartupReceiver.kt
├── ui/
│   ├── MainActivity.kt            # 首页驾驶舱
│   ├── ClusterStripActivity.kt    # 星环上屏 Activity（壁纸 only）
│   ├── ImportConfirmActivity.kt   # 或 DialogFragment
│   ├── CustomActivity.kt
│   ├── SpecActivity.kt
│   ├── AboutActivity.kt
│   └── theme/StarLiveUi.kt
└── contact/
    └── ContactConfig.kt           # assets 读微信号
```

**原则**：一个进程内 **StripOrchestrator** 是唯一「请求/释放星环」入口，UI 与 Receiver/Service 只调它。

---

## 4. 从 Lyra 的文件映射

### 4.1 建议拷贝后改编（Keep & Slim）

| Lyra 路径 | StarLive | 改编要点 |
|-----------|----------|----------|
| `settings/ClusterWallpaperSettings.kt` | `wallpaper/WallpaperRepository.kt` | 去掉 508 catalog 轮播复杂度可留接口；prefs 键改 `starlive_*`；导入文件名加 starlive_* |
| `display/ClusterDisplayController.kt` | 同名包下 | `surfaceMode` **仅 WALLPAPER**；启动 `ClusterStripActivity`；去掉 effectId |
| `display/SoftLeftFadeImageView.kt` | 原样拷 | 改 package |
| `display/WallpaperEdgeSoftener.kt` | 原样拷 | 依赖 geometry 常量改指向 `StripGeometry` |
| `display/WallpaperEdgeDrawable.kt` | 按需拷 | 随 softener |
| `display/OemClusterColors.kt` | 精简拷 | 仅 wall 相关色 |
| `effect/surface/EffectSurfaceProfile.kt` 中几何段 | `StripGeometry.kt` | **只抽常量**：STRIP 4032×284、GAUGE 1042、WALLPAPER 2990×284、feather 88/104；**不要**歌词 timing |
| `ui/LyraClusterActivity.kt` | `ClusterStripActivity` | 可继承或复制；theme 独立 |
| `ui/LyraDisplayActivity.kt` | **不要整文件** | 该文件 1100+ 行含特效；**只抽壁纸层**：layout 底图、reload broadcast、日夜 plate、soft fade 加载逻辑 → 新建精简 `ClusterStripActivity` + 可选 `StripWallpaperBinder` |
| `music/StartupReceiver.kt` | `service/StartupReceiver.kt` | 条件改「空闲显示开」非 master；启动 `KeepAliveService`；调度 `BootRecoverScheduler` |
| `music/MusicNotificationListener.kt` | `StarLiveNotificationListener.kt` | **只发 playing 布尔**；不拼歌词、不写 MusicManager 全量 |
| `music/PlaybackMonitorService.kt` | `KeepAliveService.kt` | 去掉 logcat；`startForeground` 后注册播放回调 / 确保 Listener 活着 |
| `night/NightModeManager.kt` | 可选精简 | 或 Main 内读 `UiMode` / `Configuration` |
| `ui/widget/SelectChipRail.kt` | 可选 | demo chips；否则用简单 horizontal chips |

### 4.2 明确不要拷贝

| Lyra | 原因 |
|------|------|
| `orchestrator/LyraOrchestrator.kt` 全量 | 特效/主开关/歌词；用新 `StripOrchestrator` |
| `effect/**`、`renderer/**`、`lyrics/**` | 非目标 |
| `license/**` | 非目标 |
| `music/VehicleLogcatSource.kt` | 禁止 |
| `ambient/**` | 非目标 |
| `assets/wallpaper/*` 全量 508 | 只留 4 demo + 新 catalog.json |
| `MainActivity` 全量 | 重写首页（交互终稿单页） |

### 4.3 几何真理源

```kotlin
// StripGeometry.kt — 与 Lyra EffectSurfaceProfile 对齐
const val STRIP_W = 4032
const val STRIP_H = 284
const val GAUGE_RESERVE = 1042
const val WALLPAPER_W = 2990  // 1042 + 2990 = 4032
const val WALLPAPER_H = 284
const val EDGE_FEATHER_DAY = 88
const val EDGE_FEATHER_NIGHT = 104
```

---

## 5. 核心运行时

### 5.1 StripOrchestrator 状态（逻辑）

```text
                    idlePreferOff
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    YIELDED_USER   SHOWING_WALL   YIELDED_PLAYING
    (空闲关)       (已上屏)        (播歌让出)
         │               │               │
         │          播歌 / 失败      停播+宽限
         │               ▼               │
         │          YIELDED_*  ◄─────────┘
         │               │
         └──── 空闲开+有图+!playing ──► SHOWING_WALL
```

| API（示意） | 行为 |
|-------------|------|
| `setIdlePrefer(boolean)` | 关：release + stop FGS；开：start FGS + tryShow |
| `applyCurrent(reason)` | 有图则 launch cluster wallpaper；播歌中只 pending |
| `onPlayingChanged(playing)` | true→yield；false→宽限后 tryShow |
| `scheduleBootRecover(reason)` | 空闲开才调度 2.5/8/20s |
| `releaseStrip(reason)` | finish cluster activity / surface |

### 5.2 PlaybackGate

| 规则 | 值 |
|------|-----|
| playing | 任一活跃 session `STATE_PLAYING`（或电话占用策略：playing 即让） |
| 暂停宽限 | 8_000 ms |
| 无 session / buffer 空隙 | 3_000 ms 保持「视为播放让出」 |
| 无 NLS | `playing=false` 降级（允许上屏） |

实现提示：Listener 内 `publishPlaying(Boolean)` → `PlaybackGate` → `StripOrchestrator`。  
可用 `MediaSessionManager.getActiveSessions`（需 NLS component）与 Lyra 同构。

### 5.3 BootRecoverScheduler

```text
if (!idlePrefer) return
delays = [2500, 8000, 20000]
each tick:
  if (!idlePrefer) return
  if (PlaybackGate.isEffectivelyPlaying()) return  // 保持让出
  if (hasImage) orchestrator.applyCurrent("boot#i")
```

Debounce：45s 内不重复整轮（对齐 Lyra）。

### 5.4 PendingApplyStore

```text
prefs: pending_apply = boolean
set true: 播歌中应用/导入确认成功
set false: 上屏成功 / 恢复示范
boot: 若 pending && idlePrefer → 同 apply 路径
```

### 5.5 WallpaperRepository

| 职责 | 说明 |
|------|------|
| demo catalog | `assets/wallpaper/catalog.json` 仅 4 条 |
| active 文件 | `filesDir/active_wallpaper.jpg`（及 optional `_dark`/`_light`） |
| seed | 首次 copy demo_minimal_dark |
| importFromUri / importFromDownload | 文件名序见交互 |
| prefs | `idle_prefer` 默认 true；`active_id`；`night_mode`；`first_run_hint_shown` |

**Download 扫描序**（与交互一致）：

1. `starlive_wallpaper.jpg/.png`  
2. `starlive_dark` + `starlive_light` 成对  
3. `lyra_wallpaper.jpg/.png`  
4. `cluster_wallpaper.jpg`  

### 5.6 WallpaperCropper

| 输入 | 输出 |
|------|------|
| ≈2990×284 ±2 | 原样 + bake 标记 `exact` |
| 宽≥4032 且条带高 | crop `x=1042, w=2990`，高居中 284；策略 `band` |
| 其它 | center-crop cover 2990×284；策略 `center` |

采样解码：`BitmapFactory.Options.inSampleSize`。

---

## 6. UI 层

### 6.1 Activity 清单

| 组件 | 说明 |
|------|------|
| `MainActivity` | 唯一 LAUNCHER；横屏；首页终稿布局 |
| `ClusterStripActivity` | `launchMode=singleTask`；`excludeFromRecents`；`taskAffinity` 独立；theme 无 ActionBar 全黑底 |
| `ImportConfirm*` | Activity 或全屏 Dialog；确认后回 Main |
| `CustomActivity` / `SpecActivity` / `AboutActivity` | 二级；可后续合并 |

### 6.2 主线程与刷新

- 上屏结果 / playing 变化 → `LocalBroadcast` 或 `SharedFlow` / 简单 `Listeners` 刷新胶囊  
- 壁纸文件变更 → `ClusterStripActivity` 收 `ACTION_WALLPAPER_RELOAD`（对齐 Lyra）

### 6.3 预览条

- 主屏用 **缩小版** 2990:284 比例 ImageView + 左侧灰块（约 1042/4032 宽）  
- 与实车同一 decode 路径（可降采样）以保证 WYSIWYG  

---

## 7. Manifest 清单（P0）

### 7.1 权限

```text
RECEIVE_BOOT_COMPLETED
FOREGROUND_SERVICE
FOREGROUND_SERVICE_MEDIA_PLAYBACK   // 或 specialUse 需评估；先对齐 Lyra mediaPlayback
POST_NOTIFICATIONS                 // 33+
WAKE_LOCK                          // 可选
READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE maxSdk 32
// 不要：READ_LOGS、WRITE_SECURE_SETTINGS、INTERNET（P0 默认可不加；开源链外链时再加）
```

`INTERNET`：关于页开源链接若用浏览器 intent 可不申请；若 WebView 再加。

### 7.2 组件

```text
Application: StarLiveApp
Activity: Main（exported launcher）
Activity: ClusterStrip（exported=false）
Service: KeepAliveService（FGS type mediaPlayback, exported=false, stopWithTask=false）
Service: StarLiveNotificationListener（permission BIND_NOTIFICATION_LISTENER_SERVICE）
Receiver: StartupReceiver
  - directBootAware=true
  - exported=true 仅系统 boot 时：建议 intent-filter 限 BOOT_* 等
  - 自定义 WATCHDOG：exported=false 或 signature permission（勿抄 Lyra 无鉴权 exported）
Actions: BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, USER_UNLOCKED, USER_PRESENT, MY_PACKAGE_REPLACED
  SCREEN_ON 可选
```

### 7.3 queries（可选）

```xml
<package android:name="com.lyra.cluster" />  <!-- About 检测；以实装 Lyra 包名为准 -->
```

---

## 8. 数据与存储

| Key / 路径 | 用途 |
|------------|------|
| `starlive_wallpaper` prefs | idle、night、active_id、hint、pending_apply |
| `filesDir/active_wallpaper.jpg` | 当前上屏位图 |
| `filesDir/active_dark.jpg` / `active_light.jpg` | 可选成对 |
| `assets/wallpaper/catalog.json` | 4 demo |
| `assets/contact/wechat.txt` + `qr.png` | 定制联系 |

---

## 9. 通知

| 场景 | channel | 文案 |
|------|---------|------|
| 空闲开·保持 | `starlive_keepalive` | 星澜正在保持星环壁纸 |
| 空闲开·播歌让出 | 同 channel | 星澜 · 播歌中已让出星环 |
| 点击 | → MainActivity | |

Channel 名用户可见：「星澜运行状态」。重要性：LOW（减少打扰），但仍满足 FGS。

---

## 10. 日志

| 项 | 约定 |
|----|------|
| TAG | 统一 `StarLive` |
| 级别 | 上屏/让出/boot 用 I；裁切失败 W；崩溃路径 E |
| 禁止 | 打印用户图片路径以外的隐私；不要 dump 整图 |

---

## 11. P0 任务拆分（建议实现顺序）

### Phase 0 — 工程壳（0.5–1 天）

| # | 任务 | 验收 |
|---|------|------|
| 0.1 | 创建 `android/` 工程、applicationId、空 Main 可安装 | 车机/模拟器亮标 |
| 0.2 | LICENSE Apache-2.0、占位 README | 文件存在 |
| 0.3 | 4 张 demo 资产 + catalog.json | seed 可读 |

### Phase 1 — 壁纸核心（2–3 天）

| # | 任务 | 验收 |
|---|------|------|
| 1.1 | `StripGeometry` + Edge/SoftLeft 拷贝改编 | 单元：常量正确 |
| 1.2 | `WallpaperRepository` seed/apply demo | 文件落地 |
| 1.3 | `ClusterDisplayController` + `ClusterStripActivity` 仅壁纸 | 实车空闲上屏 |
| 1.4 | `StripOrchestrator.apply/release` | 开关空闲可让出 |
| 1.5 | Main 预览条 + 三键骨架 + demo chips | 交互主路径 |

### Phase 2 — 导入与裁切（1–2 天）

| # | 任务 | 验收 |
|---|------|------|
| 2.1 | 选图 + Download 扫描 | 无相册可 Download |
| 2.2 | `WallpaperCropper` + ImportConfirm | 4032 band / center 正确 |
| 2.3 | 恢复示范确认 | prefs 清理 |

### Phase 3 — 播放让出 + FGS + 开机（2 天）

| # | 任务 | 验收 |
|---|------|------|
| 3.1 | `KeepAliveService` 通知两态 | 空闲开关起停服务 |
| 3.2 | NLS + PlaybackGate 宽限 | 播歌让出、暂停 8s |
| 3.3 | pendingApply 持久化 | 杀进程后仍生效 |
| 3.4 | StartupReceiver + BootRecover | 冷启 20s 内尽力恢复 |
| 3.5 | 无 NLS 降级 | 仍可上屏 + 黄字 |

### Phase 4 — 二级页与打磨（1 天）

| # | 任务 | 验收 |
|---|------|------|
| 4.1 | Custom / Spec / About（免责+隐私摘要+**升级 CTA**） | 文案齐全 |
| 4.2 | 日夜三段（bake + 可选双图） | 预览变 |
| 4.3 | 错误 Sheet 人话 | 交互验收自测 |
| 4.4 | `queries` Lyra 包名；升级 Sheet；文件名双兼容 | 双装提示可用 |
| 4.5 | （P0.5）handoff 导出到 Download | Lyra 可扫包导入 |

### 合计

约 **6–9 人日** 到可上车友群 debug 包（含实车调 Display）。

---

## 12. 测试计划（工程）

| 类型 | 内容 |
|------|------|
| 实车必测 | 交互文档 §14 共 13 条 |
| 模拟器 | UI/导入/裁切；**上屏可能失败**属预期 |
| 回归 | 与 Lyra 同机安装各开一次空闲/歌词 |
| 性能 | 导入 4000px 图不 OOM；FGS 不占 CPU 空转 |

---

## 13. 风险与对策

| 风险 | 对策 |
|------|------|
| Cluster Display 名称变化 | `findClusterDisplay` 多关键字 + 日志 `listDisplaysForProbe` |
| targetSdk 35 FGS 类型 | 对齐 Lyra `mediaPlayback`；注意商店政策（侧载可接受） |
| 开机被厂商杀掉 | 帮助自启动；USER_PRESENT 补刀；不承诺 100% |
| LyraDisplayActivity 难拆 | **禁止整抄**；按壁纸 surface 重写小 Activity |
| 版权 demo | 开工前替换为自有/无争议图 |

---

## 14. 后续（非 P0）

| 项 | 阶段 |
|----|------|
| 轮播 | P0.5 |
| 多图本地库 | 1.1 |
| 抽 `ring-wallpaper-core` 给 Lyra 共用 | 稳定后 |
| 检查更新 | 1.x |
| 主题包兑换码 | 方案 B |

---

## 15. 开工检查单

- [ ] 读完交互终稿决议表（§15）  
- [ ] 定 applicationId（默认 `com.starlive.app`）  
- [ ] 准备 4 张 demo 与联系方式资源  
- [ ] 初始化 git + android 工程  
- [ ] Phase 1 实车打通「应用当前」  
- [ ] Phase 3 打通播歌让出与开机恢复  

---

## 16. 修订

| 日期 | 说明 |
|------|------|
| 2026-08-03 | 初版 TECH；Lyra 映射 + P0 分期 |
| 2026-08-03 | 子项目 + 丝滑升级红线与 Phase 4.4/4.5 |
