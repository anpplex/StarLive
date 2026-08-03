# 项目结构

| 字段 | 值 |
|------|-----|
| 仓库根 | `/Users/anpple/Codex/StarLive` = https://github.com/anpplex/StarLive |
| 唯一产品工程 | **`android/`**（搭建后） |

```text
StarLive/
├── README.md
├── LICENSE                 # Apache-2.0
├── AGENTS.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── .gitignore
├── docs/                   # 权威文档（本树）
├── android/                # ★ 唯一 Android 工程（Phase 0）
└── scripts/                # 可选：装机 adb
```

## 边界

| 可改 | 不可当主线 |
|------|------------|
| `android/` · `docs/` · `scripts/` | `/Users/anpple/Codex/Lyra`（只读参考） |
| | 凯迪 APK、逆向输出（不进仓） |

详见 [AGENTS.md](../AGENTS.md) · [TECH-NOTES-1.0.md](./TECH-NOTES-1.0.md)。
