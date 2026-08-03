# 星澜 StarLive

面向阿维塔车机的 **星环空闲壁纸** 工具。开源免费，支持内置壁纸、本地导入与主题兑换；私人定制另售。播放音乐时自动暂停星环占用；需要歌词与视觉特效时，可升级至 [Lyra](https://github.com/anpplex)。

| 项目 | 说明 |
|------|------|
| 应用名称 | 星澜 / StarLive |
| 包名 | `com.starlive.app` |
| 最新版本 | [Releases](https://github.com/anpplex/StarLive/releases/latest) |
| 许可证 | [Apache-2.0](./LICENSE) |
| 源码 | https://github.com/anpplex/StarLive |

## 功能概览

- 星环空闲壁纸（旁路自绘，非系统 WallpaperService）
- 内置壁纸浏览与切换、导入裁切、本地图库
- 播放音乐时自动让出星环；可跟随车机日夜模式
- 主题兑换、版本检查、与 Lyra 的壁纸互通

## 构建与安装

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest

# 阿维塔 / 华为车机
../scripts/install-starlive-car.sh <SERIAL>
```

装机步骤、签名与权限说明见 [docs/INSTALL.md](./docs/INSTALL.md)。

## 文档

| 文档 | 内容 |
|------|------|
| [docs/INSTALL.md](./docs/INSTALL.md) | 构建、签名、车机安装 |
| [docs/PRIVACY.md](./docs/PRIVACY.md) | 隐私与权限 |
| [docs/PRODUCT_BOUNDARIES.md](./docs/PRODUCT_BOUNDARIES.md) | 能力边界 |
| [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md) | 与 Lyra 的互通约定 |
| [docs/THEME-PACK.md](./docs/THEME-PACK.md) | 主题包格式 |
| [docs/RING-WALLPAPER-CORE.md](./docs/RING-WALLPAPER-CORE.md) | 几何与裁切模块 |
| [docs/CUSTOM-SOP.md](./docs/CUSTOM-SOP.md) | 壁纸定制 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本变更记录 |

索引：[docs/README.md](./docs/README.md)

## 免责声明

本软件为第三方工具，与阿维塔、华为官方无关联。请在车辆静止时设置壁纸。系统升级可能导致功能变化或失效。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE).
