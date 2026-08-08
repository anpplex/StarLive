# StarLive 0.1.40 — Interactive crop + animated GIF/WebP

## Goals
1. Restore interactive pan/zoom crop from 0.1.38 (`CropBandView` + `ImportConfirmActivity`).
2. Full animated wallpapers: GIF + animated WebP (same pipeline).
3. Static images keep existing JPEG + left-edge bake path.

## Design

### Import / crop
- Restore `CropBandView` (drag, pinch, zoom buttons, export 2990×284).
- `ImportConfirmActivity`: load source (first frame for animated), interactive crop UI.
- On apply:
  - **Static** (jpeg/png/webp-static): export cropped bitmap → library as `.jpg` (current).
  - **Animated** (gif / animated webp): copy original bytes to library as `.gif`/`.webp`, persist crop transform (scale+tx+ty in source space or matrix floats) in index; also write active file + transform sidecar.

### Storage
- `WallpaperLibrary.Item`: add `kind: "static"|"gif"|"webp"`, optional crop fields: `cropSx, cropSy, cropSw, cropSh` (source rect normalized 0–1 or absolute ints).
- Prefer **source crop rect in image pixels** (axis-aligned) from `CropBandView` export math — already computed in `exportCropped`.
- Active: `active_wallpaper.jpg` OR `active_wallpaper.gif` OR `active_wallpaper.webp` + prefs `active_kind`, `active_crop` JSON.
- Library index backward compatible: missing kind → static from extension.

### Playback
- `ImageDecoder.decodeDrawable` → if `AnimatedImageDrawable`, start() and show on strip/home.
- Apply crop via ImageView matrix / custom clip OR pre-scale drawable bounds.
- Edge dissolve for animated: **left gradient overlay** (do not bake each frame).
- Static path unchanged: `decodeActiveForStrip` + bake.

### Gallery pick / file open
- Already `image/*`; ensure OpenDocument includes gif/webp; MediaStore lists images (gif usually included).
- DOWNLOAD_CANDIDATES add `.gif` / `.webp`.

### Version
- versionName `0.1.40`, versionCode `44`.

## Non-goals
- Video (mp4)
- Re-encoding multi-frame GIF/WebP after crop
- Carousel auto-advance of animated items mid-loop special cases
