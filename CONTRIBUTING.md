# 参与星澜 StarLive

1. [docs/GIT_WORKFLOW.md](./docs/GIT_WORKFLOW.md) — 分支、提交、PR  
2. [AGENTS.md](./AGENTS.md) — 目录边界与禁止项  
3. [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md) — 与 Lyra 兼容红线  

## 流程

```text
git checkout main && git pull
git checkout -b feature/<topic>
# 修改 android/ 与必要 docs
cd android && ./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
git commit && git push -u origin HEAD
# 开 PR → 合并 main
```

车机装机：`./scripts/install-starlive-car.sh <SERIAL>`  
验收：`docs/QA-MATRIX.md`

## 约定

- 一条 PR 一类事；Conventional Commits。  
- 勿提交密钥与 APK 二进制。  
- 几何 / 裁切 / 软边优先改 `:ring-wallpaper-core`。  
- 壁纸规格与 Lyra 兼容，勿私创分辨率。
