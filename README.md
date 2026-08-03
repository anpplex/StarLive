# 星澜 StarLive

阿维塔车机 **星环空闲壁纸** 工具：开源免费、内置示范、自定义导入；私人定制另售。播歌时自动让出星环；需要歌词特效时升级 [Lyra](https://github.com/anpplex)。

| | |
|---|---|
| 应用名 | **星澜** / StarLive |
| 包名 | `com.starlive.app` |
| 最新 Release | [v0.1.24-cache](https://github.com/anpplex/StarLive/releases/latest) |
| 协议 | [Apache-2.0](./LICENSE) |
| 仓库 | https://github.com/anpplex/StarLive |

## 功能

- 星环空闲壁纸（旁路自绘，非系统 WallpaperService）
- 内置壁纸横滑选择 · 导入裁切 · 本地图库
- 播歌自动让出 · 可选跟随车机日夜
- 主题兑换码 · 检查更新 · 导出对接 Lyra

## 快速开始

```bash
# 构建
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug

# 单元测试
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest

# 阿维塔 / 华为车机装机
../scripts/install-starlive-car.sh <SERIAL>
```

装机与权限详见 [docs/INSTALL.md](./docs/INSTALL.md)。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/INSTALL.md](./docs/INSTALL.md) | 构建 · 签名 · 车机装机 |
| [docs/PRIVACY.md](./docs/PRIVACY.md) | 隐私与权限 |
| [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md) | 与 Lyra 互通 / 升级 |
| [docs/THEME-PACK.md](./docs/THEME-PACK.md) | 主题包格式 |
| [docs/QA-MATRIX.md](./docs/QA-MATRIX.md) | 验收矩阵 |
| [docs/CUSTOM-SOP.md](./docs/CUSTOM-SOP.md) | 私人定制履约 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本记录 |
| [AGENTS.md](./AGENTS.md) | 协作者 / Agent 约定 |

完整索引：[docs/README.md](./docs/README.md)

## 免责声明

第三方工具，**与阿维塔 / 华为官方无关**。请在停车时设置壁纸。系统升级可能导致功能变化或失效。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0 — see [LICENSE](./LICENSE).
