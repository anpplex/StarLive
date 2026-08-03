# StarLive — Agent / 协作者约定

## 项目目标

在阿维塔车机交付 **星澜（StarLive）**：星环 **空闲壁纸** 专用 App。

- 免费开源 · 4 张 demo · 导入/裁切 · 播歌让出 · 开机随空闲恢复
- **Lyra 子项目**：几何/文件兼容，支持 [丝滑升级](./docs/LYRA-UPGRADE.md)
- **不做**：歌词特效、License 门闩、在线商城、软件订阅

权威：`docs/INTERACTION-1.0.md` · `docs/TECH-NOTES-1.0.md` · `docs/LYRA-UPGRADE.md`

## 目录边界（硬规则）

| 路径 | 角色 | 可否改代码发 PR |
|------|------|-----------------|
| `android/` | **唯一产品 Android 工程**（建成后） | ✅ |
| `docs/` | 权威文档 | ✅ |
| `scripts/` | 装机/工具（可选） | ✅ |
| Lyra 主仓 `/Users/anpple/Codex/Lyra` | **只读参考**（拷贝想法到本仓） | ❌ 不在本仓改 Lyra |
| 凯迪 APK / 逆向树 | 不进本仓 | ❌ 禁止提交 |

### 默认路径

```text
$STARLIVE=/Users/anpple/Codex/StarLive
$APP=$STARLIVE/android
$DOCS=$STARLIVE/docs
$LYRA_REF=/Users/anpple/Codex/Lyra/android   # 只读对照
```

构建（有工程后）：

```bash
cd $APP && ./gradlew :app:assembleDebug
```

## 禁止

1. 提交 keystore、`local.properties`、release 密钥、整包 APK。  
2. 把 Lyra 特效/License/logcat 歌词栈拷进本仓。  
3. 破坏 2990×284 / 1042 表盘 / handoff 文件名兼容（升级红线）。  
4. 在无 PR 流程下向 `main` 推业务大改（**bootstrap 首推除外**，见 GIT_WORKFLOW）。  
5. 并行 subagent **同时改同一核心文件**（见下）。

## 并行 Subagent

- **默认最大同时写代码 subagent：3**；只读探索可到 4。  
- **仅当无共享写集** 时并行（不同包/文件树；优先 worktree isolation）。  
- **必须串行**：`StripOrchestrator`、Cluster 上屏、Manifest/FGS、`WallpaperRepository` 核心。  
- **可并行示例**：静态 About/Spec 文案 vs demo 资源；只读调研 Lyra 不同目录。  
- 合并前由主会话统一构建检查。

## 版本与品牌

| 项 | 值 |
|----|-----|
| 显示名 | 星澜 / StarLive |
| applicationId（建议） | `com.starlive.app` |
| 协议 | Apache-2.0 |
| 远程 | https://github.com/anpplex/StarLive |

决策索引：`docs/DECISIONS.md`（若存在）· 交互终稿决议表。

## Git

严格遵循 [`docs/GIT_WORKFLOW.md`](./docs/GIT_WORKFLOW.md)。
