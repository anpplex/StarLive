# 星澜 StarLive

阿维塔车机 **星环屏空闲壁纸** 工具：免费开源、少量示范、自定义导入、私人定制另售。

| 项 | 值 |
|----|-----|
| 中文名 | **星澜** |
| 英文名 | **StarLive** |
| 定位 | [Lyra](https://github.com/anpplex) 生态 **子项目 / 引流层** · 可 [丝滑升级到 Lyra](./docs/LYRA-UPGRADE.md) |
| 仓库 | https://github.com/anpplex/StarLive |
| 本地路径 | `/Users/anpple/Codex/StarLive` |
| 协议 | [Apache-2.0](./LICENSE) |
| 文档 | [`docs/`](./docs/) |
| 协作者 | [`AGENTS.md`](./AGENTS.md) |
| Git 工作流 | [`docs/GIT_WORKFLOW.md`](./docs/GIT_WORKFLOW.md) |

## 产品一句话

> 装上就能换星环空闲壁纸；播歌自动让出星环；车辆启动后按「空闲显示」尽力恢复。需要歌词特效时升级 **Lyra**。

## 当前状态

- ✅ 产品 / 交互 / 技术规格（见 `docs/`）
- ✅ Android App **0.1.8-polish**（空闲壁纸、导入、播歌让出、多图库、主题兑换、检查更新、`:ring-wallpaper-core`）
- ✅ 主题包兑换（LicenseHub）与打包脚本
- ✅ Lyra ContentProvider / Download 互通
- ⬜ 实车矩阵按 [QA-MATRIX](./docs/QA-MATRIX.md) 勾选

## 文档入口

| 文档 | 说明 |
|------|------|
| [docs/README.md](./docs/README.md) | 文档索引 |
| [INSTALL](./docs/INSTALL.md) | 装机 |
| [QA-MATRIX](./docs/QA-MATRIX.md) | 验收 |
| [CUSTOM-SOP](./docs/CUSTOM-SOP.md) | 定制履约 |
| [FEATURE-ALIGNMENT](./docs/FEATURE-ALIGNMENT-1.0.md) | 功能与方案 A |
| [LYRA-UPGRADE](./docs/LYRA-UPGRADE.md) | 升级 Lyra 契约 |
| [THEME-PACK](./docs/THEME-PACK.md) | 主题包格式 |

## 免责声明

第三方工具，**与阿维塔 / 华为官方无关**。请在停车时设置壁纸。系统升级可能导致功能变化或失效。旁路自绘星环，非系统 WallpaperService。

## 构建

```bash
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

详见 [docs/INSTALL.md](./docs/INSTALL.md)。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE).
