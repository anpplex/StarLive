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
├── .github/workflows/      # CI（assembleDebug）
├── docs/                   # 权威文档
├── android/
│   ├── app/                # 产品 App com.starlive.app
│   └── ring-wallpaper-core/# 共享几何/裁切 com.starlive.ring
└── scripts/
    ├── make-theme-pack.sh  # 主题 zip
    └── install-starlive.sh # adb 装机
```

## 边界

| 可改 | 不可当主线 |
|------|------------|
| `android/` · `docs/` · `scripts/` | `/Users/anpple/Codex/Lyra`（只读参考） |
| | 凯迪 APK、逆向输出（不进仓） |

详见 [AGENTS.md](../AGENTS.md) · [TECH-NOTES-1.0.md](./TECH-NOTES-1.0.md)。
