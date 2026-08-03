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
