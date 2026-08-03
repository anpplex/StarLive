# 星澜装机说明（车机 / 开发）

## 1. 构建

```bash
cd /Users/anpple/Codex/StarLive/android
./gradlew :app:assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Release 需自备签名配置（不入库）。

## 2. adb 安装（USB 调试）

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.starlive.app/.ui.MainActivity
```

部分车机需先允许「USB 安装」或使用厂商调试桥。

## 3. U 盘 / 文件管理器

1. 将 APK 拷到车机可访问存储  
2. 用文件管理器打开安装（需允许未知来源）  
3. 首次打开星澜 → 按提示点「应用当前」  

## 4. 权限建议

| 权限 | 用途 |
|------|------|
| 通知使用权 | 播歌让出更准（可选） |
| 忽略电池优化 | 开机恢复更稳（关于页入口） |
| 存储 / 相册 | 导入壁纸 |
| 网络 | **仅兑换主题**时需要 |

## 5. 与 Lyra 双装

1. 两 App **可并排安装**（不同 applicationId）  
2. 星澜开「已装 Lyra 时让路」  
3. Lyra 壁纸 → **下载导入**（优先 ContentProvider）  
4. 详见 [LYRA-UPGRADE.md](./LYRA-UPGRADE.md)  

## 6. 兑换服务

- API 默认：`https://buy.998618.xyz`  
- 覆盖：`./gradlew :app:assembleDebug -PREDEEM_API_BASE=https://host`  

## 7. 卸载

卸载清除本机壁纸库与 device_id；已绑定的兑换码 **不会** 因卸载自动释放（一码一设备）。  
