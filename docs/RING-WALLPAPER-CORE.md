# ring-wallpaper-core

| 项目 | 值 |
|------|-----|
| 模块 | `android/ring-wallpaper-core` |
| 包名 | `com.starlive.ring` |
| 用途 | 星环壁纸几何与像素处理（与上屏逻辑解耦） |

## 职责

| 类型 | 说明 |
|------|------|
| `StripGeometry` | 4032×284 / 表盘 1042 / 内容 2990×284 / feather 88·104 |
| `CropStrategy` | 尺寸策略：EXACT / BAND / CENTER |
| `WallpaperCropper` | 解码并按策略裁切 |
| `WallpaperEdgeSoftener` | 左缘软边 |

不包含：Cluster 上屏、前台服务、兑换、歌词特效。

## 接入

```kotlin
implementation(project(":ring-wallpaper-core"))
```

```bash
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

## 与 Lyra

壁纸段尺寸须与 Lyra 一致，见 [LYRA-UPGRADE.md](./LYRA-UPGRADE.md)。修改几何常量时请同步核对 Lyra 侧 profile。
