# 参与星澜 StarLive

感谢关注。开发请先阅读：

1. [docs/GIT_WORKFLOW.md](./docs/GIT_WORKFLOW.md) — 分支、提交、PR  
2. [AGENTS.md](./AGENTS.md) — 目录边界与禁止项  
3. [docs/TECH-NOTES-1.0.md](./docs/TECH-NOTES-1.0.md) — 技术分期  
4. [docs/LYRA-UPGRADE.md](./docs/LYRA-UPGRADE.md) — 升级 Lyra 红线  

## 快速流程

```text
git checkout main && git pull
git checkout -b feature/<topic>
# 修改 android/ 与必要 docs
cd android && ./gradlew :app:assembleDebug
git commit && git push -u origin HEAD
# 开 PR → 合并 main
```

## 行为准则

- 一条 PR 一类事；Conventional Commits。  
- 勿提交密钥与 APK 二进制。  
- 壁纸几何与 Lyra 兼容，勿私创分辨率。  
