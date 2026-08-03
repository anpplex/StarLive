# 路线图

## P0 · MVP

| Phase | 内容 | 状态 |
|-------|------|------|
| 0 | 文档 bootstrap + git main 推远程 | ✅ 2026-08-03 |
| 0b | `android/` 空壳可 `assembleDebug` | ✅ 2026-08-03 |
| 1 | 几何/软边 · Repository · Cluster 上屏 · 首页三键+demo | ✅ 2026-08-03 |
| 2 | 导入 · Cropper · 确认页 · 恢复示范 | ✅ 2026-08-03 |
| 3 | FGS · 播歌让出 · pending · 开机恢复 | ✅ 2026-08-03 |
| 4 | 定制/规格/关于 · 升级 CTA · Lyra queries · handoff 导出 | ✅ 2026-08-03 (0.1.0-p0) |

## P0.5

| 项 | 状态 |
|----|------|
| 示范轮播 + 间隔 | ✅ 0.1.1-p05 |
| 检测 Lyra 自动让路（可关） | ✅ 0.1.1-p05 |
| handoff 导出（Phase 4 已有） | ✅ |
| 电池优化 / 通知使用权入口 | ✅ 关于页 |
| 日夜双图示范主题（影像级 demo） | ✅ 0.1.3-demo |
| handoff 同步导出到 Download 根目录（Lyra 可扫） | ✅ 0.1.2-p05 |

## 1.1+

| 项 | 状态 |
|----|------|
| 多图本地库（导入保留、切换、长按删） | ✅ 0.1.4-lib |
| ContentProvider handoff（星澜侧） | ✅ 0.1.4-lib `content://com.starlive.app.handoff/*` |
| Lyra「从星澜 ContentProvider 导入」 | ⬜（Lyra 仓配合） |
| 主题包兑换码（App 端 + LicenseHub API） | ✅ 0.1.5-redeem（`feature/theme-pack-redeem`） |
| 主题包格式文档 + `make-theme-pack.sh` | ✅ 见 `docs/THEME-PACK.md` |
| LicenseHub Admin「星澜兑换」发码 | ✅（LicenseHub 仓） |
| LicenseHub 兑换码作废 + 生产部署 | ✅ Admin 作废；DEMO 码应废止 |
| LicenseHub 登记主题包（磁盘 zip → 库表 SHA） | ✅ Admin「登记主题包」 |
| 兑换后一键应用上屏 | ✅ 0.1.6-redeem |
| GIF / 视频壁纸 | ❌ 暂缓（产品演进备忘） |
| ring-wallpaper-core 共享 | ⬜ |

分支命名对齐：`feature/p0-phase1-strip-display` 等。见 [GIT_WORKFLOW.md](./GIT_WORKFLOW.md) · [TECH-NOTES-1.0.md](./TECH-NOTES-1.0.md)。
