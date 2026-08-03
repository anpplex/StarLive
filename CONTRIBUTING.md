# 参与贡献

## 构建

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest
```

## 约定

- 自 `main` 开分支，PR 合并  
- 勿提交密钥、`local.properties`、整包 APK  
- 星环几何（2990×284 / 表盘 1042）优先改 `:ring-wallpaper-core`  
- 壁纸定制说明：[docs/CUSTOM-SOP.md](./docs/CUSTOM-SOP.md)  
