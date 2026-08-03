# 参与贡献

感谢关注星澜。提交代码前请阅读：

- [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md) — 与 Lyra 的规格兼容要求  
- [docs/RING-WALLPAPER-CORE.md](./docs/RING-WALLPAPER-CORE.md) — 几何与裁切约束  
- [docs/PRODUCT_BOUNDARIES.md](./docs/PRODUCT_BOUNDARIES.md) — 产品边界  

## 流程

1. 自 `main` 创建功能分支  
2. 修改 `android/` 与必要文档  
3. 本地通过构建与单元测试：

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
./gradlew :ring-wallpaper-core:testDebugUnitTest :app:testDebugUnitTest
```

4. 提交并开启 Pull Request  

车机安装：`./scripts/install-starlive-car.sh <SERIAL>`

## 约定

- 单个 PR 聚焦一类变更；提交信息使用 Conventional Commits  
- 勿提交密钥、`local.properties`、整包 APK  
- 几何、裁切、软边优先修改 `:ring-wallpaper-core`  
- 壁纸规格须与 Lyra 兼容，勿擅自更改分辨率或表盘安全区  
