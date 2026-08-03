# StarLive Android

| 项 | 值 |
|----|-----|
| applicationId | `com.starlive.app` |
| minSdk | 28 |
| 文档 | `../docs/TECH-NOTES-1.0.md` |

## 模块

| 模块 | 说明 |
|------|------|
| `:app` | 星澜产品 App |
| `:ring-wallpaper-core` | 共享几何 / 裁切 / 软边（`com.starlive.ring`） |

```bash
./gradlew :ring-wallpaper-core:assembleDebug :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`（不进 Git）。  
Core 说明：`../docs/RING-WALLPAPER-CORE.md`。
