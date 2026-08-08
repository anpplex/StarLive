# 星澜 StarLive

面向 **阿维塔车机星环屏** 的壁纸工具。

在车机上为星环空闲状态更换壁纸：内置图、本地导入与裁切、主题兑换；播放音乐时自动让出星环。需要歌词与播歌特效时，可配合 [Lyra](https://github.com/anpplex) 使用。

| 项目 | 说明 |
|------|------|
| 名称 | 星澜 / StarLive |
| 包名 | `com.starlive.app` |
| 适配 | 阿维塔车机星环（4032×284，壁纸带 2990×284） |
| 版本 | [Releases](https://github.com/anpplex/StarLive/releases/latest) |
| 协议 | [Apache-2.0](./LICENSE) |

## 功能

- 星环空闲壁纸显示（旁路自绘）
- 内置壁纸横滑切换
- 从车机媒体库 / 文件导入；交互裁切（拖动 / 缩放）为 2990×284
- 支持 JPEG / PNG 静态图与 **GIF / 动态 WebP** 动图壁纸
- 播放音乐时自动暂停星环占用
- 已装 Lyra 时默认 **Lyra 优先** 让出星环
- 跟随车机日夜模式
- 主题兑换、壁纸定制（见 [docs/CUSTOM-SOP.md](./docs/CUSTOM-SOP.md)）

## 构建

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

Debug APK：`android/app/build/outputs/apk/debug/app-debug.apk`

### Release 包命名

统一为 **`StarLive-<versionName>.apk`**（例：`StarLive-0.1.41.apk`）：

```bash
cd android && ./gradlew :app:assembleRelease
./scripts/package-release.sh          # → releases/StarLive-0.1.41.apk
# 或一步构建+打包：
./scripts/package-release.sh --build
```

单元测试：

```bash
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest
```

更多版本说明见 [CHANGELOG.md](./CHANGELOG.md)。

## 免责声明

第三方工具，与阿维塔、华为官方无关。请在车辆静止时设置壁纸。系统升级可能导致功能变化或失效。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE).
