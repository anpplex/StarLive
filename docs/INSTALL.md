# 安装说明

## 构建

```bash
cd android
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

通用设备：

```bash
./scripts/install-starlive.sh --build --launch
```

阿维塔 / 华为车机（直接 `adb install` 可能因厂商校验失败）：

```bash
cd android && ./gradlew :app:assembleDebug
./scripts/install-starlive-car.sh <SERIAL>
```

脚本会临时处理安装器限制，并以厂商允许的方式安装到车机前台用户（通常为 user 12）。

### Download 导入注意

车机前台用户与 `adb push` 默认用户可能不一致。请将图片放入当前登录用户的 Download，或通过应用内文件选择器导入。

### Release 签名

```bash
cd android
# 配置 keystore.properties（参考 keystore.properties.example，勿提交密钥）
../scripts/build-release.sh
# 可选：发布到 GitHub Releases
../scripts/publish-github-release.sh vX.Y.Z
```

## 权限

| 权限 | 用途 |
|------|------|
| 通知使用权 | 更准确判断是否在播放音乐（可选） |
| 忽略电池优化 / 自启动 | 提高开机后自动恢复成功率 |
| 存储 / 相册 | 导入壁纸 |
| 网络 | 仅兑换主题与检查更新时使用 |

未加入厂商自启动白名单时，冷启动后可能不会自动恢复星环显示；打开星澜一次即可恢复。

## 与 Lyra 并存

1. 两应用可同时安装（包名不同）  
2. 在星澜中开启「Lyra 优先」  
3. 在 Lyra 中导入壁纸（支持读取星澜当前图）  
4. 详见 [LYRA-UPGRADE.md](./LYRA-UPGRADE.md)  

## 兑换服务

- 默认 API：`https://buy.998618.xyz`  
- 覆盖：`./gradlew :app:assembleDebug -PREDEEM_API_BASE=https://host`  

## 卸载

卸载会清除本机壁纸库与设备标识。已绑定的兑换码不会因卸载自动释放（一码一设备）。
