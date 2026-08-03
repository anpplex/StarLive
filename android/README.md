# StarLive Android

| 项目 | 值 |
|------|-----|
| applicationId | `com.starlive.app` |
| minSdk | 28 |
| 模块文档 | [../docs/RING-WALLPAPER-CORE.md](../docs/RING-WALLPAPER-CORE.md) |

## 模块

| 模块 | 说明 |
|------|------|
| `:app` | 星澜应用 |
| `:ring-wallpaper-core` | 几何、裁切与软边（`com.starlive.ring`） |

```bash
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

调试包输出：`app/build/outputs/apk/debug/app-debug.apk`（不纳入版本库）。
