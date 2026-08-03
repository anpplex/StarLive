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
- 从车机媒体库 / 文件导入，支持拖动缩放裁切
- 播放音乐时自动暂停星环占用
- 跟随车机日夜模式
- 主题兑换、壁纸定制（见 [docs/CUSTOM-SOP.md](./docs/CUSTOM-SOP.md)）

## 构建

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`

单元测试：

```bash
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest
```

更多版本说明见 [CHANGELOG.md](./CHANGELOG.md)。

## 车机安装（阿维塔 / 华为）

普通 `adb install` 会失败（`INSTALL_FAILED_INTERNAL_ERROR`）。需旁路安装：

```bash
SERIAL=<你的序列号>
APK=android/app/build/outputs/apk/debug/app-debug.apk
REMOTE=/data/local/tmp/starlive.apk
PKG=com.starlive.app

adb -s "$SERIAL" push "$APK" "$REMOTE"
adb -s "$SERIAL" shell pm disable-user --user 12 com.android.packageinstaller
adb -s "$SERIAL" shell pm disable-user --user 0 com.android.packageinstaller || true
adb -s "$SERIAL" shell pm install -r -d -g -t -i com.huawei.appinstaller.car --user 12 "$REMOTE"
adb -s "$SERIAL" shell pm install -r -d -g -t -i com.huawei.appinstaller.car --user 0 "$REMOTE" || true
adb -s "$SERIAL" shell pm enable --user 12 com.android.packageinstaller || true
adb -s "$SERIAL" shell pm enable --user 0 com.android.packageinstaller || true
adb -s "$SERIAL" shell rm -f "$REMOTE"
adb -s "$SERIAL" shell pm enable --user 12 "$PKG" || true
adb -s "$SERIAL" shell am start --user 12 -n "$PKG/.ui.MainActivity" || true
```

也可直接从 [Releases](https://github.com/anpplex/StarLive/releases/latest) 下载 APK，再按上式安装。

## 免责声明

第三方工具，与阿维塔、华为官方无关。请在车辆静止时设置壁纸。系统升级可能导致功能变化或失效。

## 许可

Copyright © StarLive contributors.  
Licensed under the Apache License, Version 2.0. See [LICENSE](./LICENSE).
