# 星澜 StarLive

阿维塔车机 **星环屏空闲壁纸** 工具：免费开源、少量示范、自定义导入、私人定制另售。

| 项 | 值 |
|----|-----|
| 中文名 | **星澜** |
| 英文名 | **StarLive** |
| 定位 | [Lyra](https://github.com/anpplex) 生态 **子项目 / 引流层** · 可 [丝滑升级到 Lyra](./docs/LYRA-UPGRADE.md) |
| 仓库 | https://github.com/anpplex/StarLive |
| 最新版本 | **0.1.24-cache** · [Releases](https://github.com/anpplex/StarLive/releases) |
| 协议 | [Apache-2.0](./LICENSE) |
| 文档 | [`docs/`](./docs/) |
| 协作者 | [`AGENTS.md`](./AGENTS.md) |
| Git 工作流 | [`docs/GIT_WORKFLOW.md`](./GIT_WORKFLOW.md) |

## 产品一句话

> 装上就能换星环空闲壁纸；播歌自动让出星环；车辆启动后按「空闲显示」尽力恢复。需要歌词特效时升级 **Lyra**。

## 当前状态

- ✅ 产品 / 交互 / 技术规格（见 `docs/`）
- ✅ Android App：空闲壁纸、导入裁切、播歌让出、多图库、主题兑换、检查更新、`ring-wallpaper-core`
- ✅ 冷启补偿：进程启动自动恢复（部分车机不投递 BOOT，见 [QA-MATRIX §B1](./docs/QA-MATRIX.md)）
- ✅ 主题包兑换（LicenseHub）与打包 / 发版脚本
- ✅ Lyra ContentProvider / Download 互通
- ✅ CI：assembleDebug + unit tests（裁切 / 兑换 / PlaybackGate）
- ✅ 远端日夜跟随（`ui_night_mode`）+ 星环羽化交叉淡入
- ✅ 播歌让出过滤壁纸引擎假阳性（0.1.22）
- ✅ 首页预览与星环同源烘焙 + 羽化缓存
- ✅ 实车矩阵大部通过（[QA-MATRIX](./docs/QA-MATRIX.md)）；剩余 I1 完整选图 / M3 实车切歌 / 真音乐让出回归需人工；0.1.22 已过滤 Wallpaper Engine 假让出

## 快速开始

```bash
# Debug 构建
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug

# 单元测试
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest

# 车机装机（华为 / 阿维塔旁路）
../scripts/install-starlive-car.sh <SERIAL>

# 签名 Release（需 android/keystore.properties）
../scripts/build-release.sh
```

装机与权限：[docs/INSTALL.md](./docs/INSTALL.md)

## 文档入口

| 文档 | 说明 |
|------|------|
| [docs/README.md](./docs/README.md) | 文档索引 |
| [INSTALL](./docs/INSTALL.md) | 装机 · 签名 · 权限 |
| [QA-MATRIX](./docs/QA-MATRIX.md) | 验收矩阵 |
| [CUSTOM-SOP](./docs/CUSTOM-SOP.md) | 定制履约 |
| [LYRA-UPGRADE](./docs/LYRA-UPGRADE.md) | 升级 Lyra 契约 |
| [THEME-PACK](./docs/THEME-PACK.md) | 主题包格式 |
| [RING-WALLPAPER-CORE](./docs/RING-WALLPAPER-CORE.md) | 共享几何模块 |

## 免责声明

第三方工具，**与阿维塔 / 华为官方无关**。请在停车时设置壁纸。系统升级可能导致功能变化或失效。旁路自绘星环，非系统 WallpaperService。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE).
