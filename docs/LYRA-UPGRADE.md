# 星澜与 Lyra 互通

| 项目 | 说明 |
|------|------|
| 星澜 | 空闲壁纸（`com.starlive.app`） |
| Lyra | 歌词特效与完整星环能力 |
| 目标 | 壁纸规格兼容、可迁移、双装时不争抢星环 |

## 几何与文件（冻结）

| 项 | 值 |
|----|-----|
| 壁纸带 | **2990×284** |
| 全条 | 4032×284，左侧表盘保留 **1042** px |
| 软边羽化 | 日 88 / 夜 104（见 `ring-wallpaper-core`） |
| Download 文件名 | `starlive_wallpaper.*`、`lyra_wallpaper.*`、`cluster_wallpaper.*` |

勿另立分辨率或表盘边距，以免定制图与 handoff 无法复用。

## 迁移方式

| 方式 | 说明 |
|------|------|
| ContentProvider | `content://com.starlive.app.handoff/active`（JPEG）、`…/meta`（元数据） |
| 导出 | 升级页「导出壁纸到 Download/StarLive」 |
| 文件约定 | 两边扫描上述 Download 文件名 |

handoff 元数据格式：`starlive-handoff/v1`（含 `activeId`、`idlePrefer`、`nightMode` 等）。

## 双装

| 情况 | 建议 |
|------|------|
| 已装 Lyra 且开启「Lyra 优先」 | 星澜不占用空闲星环 |
| 仅装 Lyra、未开总开关 | 星澜可继续空闲壁纸 |
| 播歌 | 星澜让出；特效由 Lyra 负责 |

## 工程约束

1. 几何与裁切与 `ring-wallpaper-core` / Lyra 壁纸段一致  
2. 应用 ID 保持独立，可与 Lyra 并排安装  
3. 星澜不含 Lyra 授权密钥；升级入口仅深链或外链  

详见 [RING-WALLPAPER-CORE.md](./RING-WALLPAPER-CORE.md)、[PRODUCT_BOUNDARIES.md](./PRODUCT_BOUNDARIES.md)。
