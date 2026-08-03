# StarLive — Agent / 协作者约定

## 项目目标

在阿维塔车机交付 **星澜（StarLive）**：星环 **空闲壁纸** 专用 App。

- 免费开源 · 内置示范 · 导入/裁切 · 播歌让出 · 开机随空闲恢复
- **Lyra 子项目**：几何/文件兼容，见 [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md)
- **不做**：歌词特效、License 门闩、在线商城、软件订阅

权威规格：`docs/INTERACTION-1.0.md` · `docs/TECH-NOTES-1.0.md` · `docs/LYRA-UPGRADE.md`

## 目录边界

| 路径 | 角色 | 可否改代码发 PR |
|------|------|-----------------|
| `android/` | **唯一产品 Android 工程** | ✅ |
| `docs/` | 文档 | ✅ |
| `scripts/` | 装机 / 打包工具 | ✅ |
| Lyra 主仓（本地参考） | **只读** | ❌ 不在本仓改 Lyra |
| 第三方 APK / 逆向树 | 不进本仓 | ❌ |

```text
$STARLIVE = 本仓库根
$APP      = $STARLIVE/android
```

```bash
cd android && ./gradlew :app:assembleDebug
./scripts/install-starlive-car.sh <SERIAL>
```

## 禁止

1. 提交 keystore、`local.properties`、release 密钥、整包 APK。  
2. 把 Lyra 特效 / License / 歌词栈拷进本仓。  
3. 破坏 2990×284 / 1042 表盘 / handoff 文件名兼容（升级红线）。  
4. 无 PR 向 `main` 推业务大改。  
5. 并行 subagent **同时改同一核心文件**。

## 并行 Subagent

- 默认最多 **3** 个写代码 subagent；只读探索可到 4。  
- **必须串行**：`StripOrchestrator`、Cluster 上屏、Manifest/FGS、`WallpaperRepository`。  
- 合并前由主会话统一构建检查。

## 版本与品牌

| 项 | 值 |
|----|-----|
| 显示名 | 星澜 / StarLive |
| applicationId | `com.starlive.app` |
| 协议 | Apache-2.0 |
| 远程 | https://github.com/anpplex/StarLive |

决议摘要：`docs/DECISIONS.md`。Git：`docs/GIT_WORKFLOW.md`。

## 工作节奏

- 按规格与用户指令持续推进；小改与 Phase 内实现可直接做。  
- **暂停询问**：Bug 策略歧义、改产品边界 / 升级契约 / 加订阅联网等。
