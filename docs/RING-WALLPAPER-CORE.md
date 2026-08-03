# ring-wallpaper-core

| 字段 | 值 |
|------|-----|
| 模块 | `android/ring-wallpaper-core` |
| 包名 | `com.starlive.ring` |
| 消费端 | StarLive App（当前）；Lyra 可选后续依赖同一 AAR / 源码拷贝 |
| 状态 | **0.1.7-core** 起 StarLive 内建 |

## 1. 职责

与产品线无关的 **星环条壁纸几何与像素处理**：

| 类型 | 说明 |
|------|------|
| `StripGeometry` | 4032×284 / 表盘 1042 / 内容 2990×284 / feather 88·104 |
| `CropStrategy` | 纯尺寸 → EXACT / BAND / CENTER（无 Bitmap，可 JVM 单测） |
| `WallpaperCropper` | decode + 按 [CropStrategy] 裁切 Bitmap |
| `WallpaperEdgeSoftener` | 左缘 soft dissolve bake |

**不包含：** 上屏 Activity、FGS、兑换、Lyra 特效、License。

## 2. 为何独立模块

- 升级契约要求星澜与 Lyra **同一像素规格**（[LYRA-UPGRADE.md](./LYRA-UPGRADE.md)）  
- TECH 规划「稳定后抽 core」；先在 StarLive 仓落地，避免过早 monorepo  
- Lyra 可：① 继续本地拷贝常量 ② 日后 `implementation` 同源 AAR / 子模块  

## 3. 接入

StarLive：

```kotlin
// android/app/build.gradle.kts
implementation(project(":ring-wallpaper-core"))

import com.starlive.ring.StripGeometry
import com.starlive.ring.WallpaperCropper
import com.starlive.ring.WallpaperEdgeSoftener
```

构建：

```bash
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

## 4. 与 Lyra 对齐检查

| 常量 | StarLive core | Lyra `EffectSurfaceProfile` |
|------|---------------|-----------------------------|
| 全条宽 | 4032 | 4032 |
| 表盘 | 1042 | `GAUGE_RESERVE_PHYSICAL_PX` |
| 壁纸带 | 2990×284 | `WALLPAPER_PHYSICAL_WIDTH` × 284 |
| feather | 88 / 104 | 同源 bake |

改 core 几何时 **必须** 同步核对 Lyra profile，并跑 handoff 导入验收。

## 5. 非目标

- 不发布到 Maven Central（除非日后产品化）  
- 不塞进特效 / 歌词  
- 不强制 Lyra 立刻改依赖（可渐进）  
