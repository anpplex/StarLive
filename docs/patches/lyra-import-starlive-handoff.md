# Lyra patch: StarLive handoff import

Applied in Lyra branch `fix/starlive-handoff-import` on file:

`android/app/src/main/java/com/lyra/cluster/settings/ClusterWallpaperSettings.kt`

## Behavior

`importFromDownloadCandidates` also scans:

| Path | Names |
|------|--------|
| `Download/StarLive/` | `lyra_wallpaper.*`, `starlive_wallpaper.*`, `active_wallpaper.jpg`, … |
| `Download/` | same names (StarLive export writes both) |

## StarLive export

Upgrade → 导出壁纸 writes:

- `Download/StarLive/handoff.json`
- `Download/StarLive/lyra_wallpaper.jpg`
- `Download/lyra_wallpaper.jpg` (root, for older Lyra)

## User flow

1. StarLive: 升级到 Lyra → 导出壁纸  
2. Lyra: 壁纸 → 下载导入  
3. Optional: close StarLive idle / enable yield-to-Lyra  

## ContentProvider（星澜 0.1.4+ · Lyra 已对接）

| URI | 类型 |
|-----|------|
| `content://com.starlive.app.handoff/active` | JPEG openFile 只读 |
| `content://com.starlive.app.handoff/meta` | Cursor：format / activeId / idlePrefer / nightMode / version |

### Lyra 实现

`ClusterWallpaperSettings.importFromStarLiveProvider()` + `importFromDownloadCandidates()`：

1. 若已装 `com.starlive.app` → 读 ContentProvider（并尽量同步 `idlePrefer`）  
2. 否则扫 `Download/StarLive/` 与 `Download/` 约定文件名  

Manifest（Lyra）需：

```xml
<queries>
  <package android:name="com.starlive.app" />
  <provider android:authorities="com.starlive.app.handoff" />
</queries>
```

壁纸面板「下载导入」即走上述 best-effort 路径。
