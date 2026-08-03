# 星澜与 Lyra 互通

| 项目 | 说明 |
|------|------|
| 星澜 | 空闲壁纸工具 |
| Lyra | 歌词特效与完整座舱能力 |
| 目标 | 升级或并存时保留壁纸、避免双端争抢星环 |

相关：[PRODUCT_BOUNDARIES.md](./PRODUCT_BOUNDARIES.md) · [RING-WALLPAPER-CORE.md](./RING-WALLPAPER-CORE.md)

---

## 1. 概述

星澜提供空闲壁纸；用户升级或同时安装 Lyra 后，应能继续使用已有壁纸，并由一端接管星环显示。

---

## 2. 能力对照

| 维度 | 星澜 | Lyra |
|------|------|------|
| 空闲壁纸 | 主能力 | 支持导入 / 沿用 |
| 播歌 | 让出星环 | **特效占屏**（总开关开时） |
| 授权 | 无 | LicenseHub |
| 仓库 | `anpplex/StarLive` | Lyra 主仓；日后可抽 shared core |

---

## 3. 互通验收

| ID | 要求 | 说明 |
|----|------|------|
| U1 | 壁纸可迁移 | Lyra 可通过 ContentProvider 或 Download 约定路径读取星澜当前图 |
| U2 | 开关语义一致 | 「星环壁纸 / 空闲显示」语义两端对齐或引导用户同步一次 |
| U3 | 双装不争抢 | 星澜检测到 Lyra 时可开启「Lyra 优先」并释放星环 |
| U4 | 播歌分工 | 星澜让出星环；播歌特效由 Lyra 负责 |
| U5 | 入口连续 | 星澜提供升级说明与导出入口 |
| U6 | 定制规格通用 | 定制图 2990×284 同时适用于星澜与 Lyra |

---

## 4. 数据契约（1.0 必须遵守 · 升级地基）

### 4.1 壁纸像素与命名（冻结）

| 项 | 契约 |
|----|------|
| 内容区 | **2990×284** |
| 全条 | 4032×284，左 **1042** 表盘保留 |
| Download 兼容名 | `starlive_wallpaper.*` **以及** `lyra_wallpaper.*` / `cluster_wallpaper.*`（两边互相认） |
| 日夜对 | `*_dark` / `*_light` 命名规则两边一致 |
| 软边 | feather 88/104 与 Lyra 几何同源（`StripGeometry` = Lyra profile 子集） |

**禁止**：星澜私创另一套分辨率/边距导致 Lyra 无法复用定制图。

### 4.2 可迁移状态（建议导出清单）

星澜应能生成（1.0 可先 **手动目录约定**，1.1 一键导出）：

```text
# 逻辑清单 — 实现可用 JSON + 文件
{
  "format": "starlive-handoff/v1",
  "activeId": "custom" | "demo_...",
  "idlePrefer": true,
  "nightMode": "auto" | "dark" | "light",
  "pendingApply": false,
  "files": {
    "active": "active_wallpaper.jpg",
    "dark": "active_dark.jpg?",
    "light": "active_light.jpg?"
  }
}
```

| 落盘位置（1.0 务实） | 说明 |
|----------------------|------|
| App 私有 `filesDir` | 主存储 |
| **可选镜像**到 `Download/StarLive/handoff/` | 便于 Lyra 无权限读私有目录时仍可导入；用户可见需写清 |

### 4.3 Prefs 键命名空间

| 建议 | 说明 |
|------|------|
| 星澜 prefs 名 | `starlive_wallpaper` 等，**加前缀**，避免与 Lyra 冲突 |
| Lyra 侧 | 升级导入时 **映射**到 `lyra_cluster_wallpaper` 等现有键，不要求星澜直接写 Lyra 包名 prefs（Android 跨应用默认不可写） |

跨应用迁移手段（按实现难度）：

| 手段 | 1.0 | 说明 |
|------|-----|------|
| A. 共享 Download 约定文件 | ✅ 必做 | 两边都扫同一文件名 |
| B. 星澜「导出给 Lyra」按钮 → 写 Download/handoff | ✅ 建议 P0.5 | 用户一键 |
| C. ContentProvider exported + 签名/权限 | 1.1 | 真自动 |
| D. 同签名 sharedUserId | ❌ 不推荐 | 绑定过死 |

**1.0 丝滑底线 = A + 文案引导；目标丝滑 = A + B；完全自动 = C（Lyra 版本配合）。**

---

## 5. 双装共存（升级过渡期）

用户常会 **暂时两套都装**。

| 规则 | 行为 |
|------|------|
| 星澜检测 Lyra | `PackageManager` + `queries`；About/首页可显示「已安装 Lyra」 |
| **Lyra 总开关开** 或 **Lyra 进程占星环**（能探则探） | 星澜：**停止空闲上屏与 boot 抢占**，胶囊「已移交 Lyra」类文案；FGS 可停或极简 |
| 仅安装 Lyra 但未开总开关 | 星澜可继续显示空闲壁纸 |
| 播歌 | 星澜始终让出（已有）→ 与 Lyra 特效兼容 |
| 用户点「升级到 Lyra」 | 见 §6 |

探测失败时：不误杀星澜能力；帮助写「若两套同时开空闲，请只开一边」。

---

## 6. 升级交互（星澜侧）

### 6.1 入口

| 位置 | 文案示例 |
|------|----------|
| Footer | **升级到 Lyra 歌词特效**（主升级链，强于纯说明） |
| About | 产品关系 + 升级步骤 |
| 首次播歌让出后（可选，不烦） | 弱提示一次：「想要歌词特效？了解 Lyra」 |

### 6.2 升级 Sheet / 页（建议结构）

```text
标题：升级到 Lyra
正文：
  星澜负责空闲壁纸；Lyra 在播歌时显示特效歌词，并可接管壁纸。
  你的壁纸规格与 Lyra 通用。

步骤：
  1. 安装 Lyra（按钮：复制下载链接 / 打开说明页）
  2. 在 Lyra 完成授权并打开「Lyra 总开关」
  3. 【导出壁纸供 Lyra】（P0.5）或「请保持 Download 中的 starlive/lyra_wallpaper」
  4. （可选）关闭星澜「空闲显示」或卸载星澜，避免双开

[ 导出壁纸包 ]  [ 我已安装 Lyra ]
[ 关闭 ]
```

「我已安装 Lyra」：若检测到包 → 尝试 deep link / 启动 Lyra 主界面；并建议星澜 yield。

### 6.3 Lyra 侧（主仓后续，本文约束对方）

Lyra 应增加（版本规划，不阻塞星澜 1.0）：

| 能力 | 说明 |
|------|------|
| 导入 handoff/v1 或扫描 Download 星澜文件名 | 一键「从星澜导入壁纸」 |
| 设置文案 | 「来自星澜的用户：…」 |
| 安装后若读到 handoff | 可选通知「已导入星澜壁纸」 |

---

## 7. 工程约束（StarLive 实现红线）

| # | 红线 |
|---|------|
| 1 | 几何与 feather **不得偏离** Lyra `EffectSurfaceProfile` 壁纸段 |
| 2 | 导入文件名 **保持 lyra_wallpaper 兼容** |
| 3 | 裁切输出必须是 Lyra 可直接 `importFromFile` 的位图 |
| 4 | 1.0 可独立拷贝代码，但 **标注 handoff 契约版本** `starlive-handoff/v1` |
| 5 | 日后抽 `ring-wallpaper-core` 时，两边依赖同一 artifact，升级更稳 |
| 6 | applicationId 保持独立（`com.starlive.app`）；**不要**做成 Lyra 的 product flavor 以致无法并排安装（并排有利于过渡周） |
| 7 | 升级 CTA 与购买 Lyra 的链接可配置（assets/BuildConfig），勿散落魔数 |
| 8 | 开源星澜 **不包含** Lyra 授权密钥；升级页只深链/外链 |

---

## 8. 分期

| 阶段 | 星澜 | Lyra |
|------|------|------|
| **P0** | 兼容文件名；Footer 升级说明；queries 检测；双开帮助文案；几何同源 | 文档承诺后续导入 |
| 后续 | 导出壁纸包到 Download/StarLive；检测 Lyra 后一键释放星环 | Lyra 设置项「从星澜 / Download 导入」 |
| **1.1** | ContentProvider handoff；升级 Sheet 完整 | 自动发现 Provider；可选迁移通知 |
| **稳定后** | shared Maven/module core | 同 core |

---

## 9. 文案（定稿级）

| ID | 中文 |
|----|------|
| upgrade_title | 升级到 Lyra |
| upgrade_body | 星澜负责空闲壁纸；Lyra 增加播歌特效歌词。壁纸尺寸与导入方式通用，可平滑升级。 |
| upgrade_cta_footer | 升级到 Lyra 歌词特效 |
| upgrade_export | 导出壁纸供 Lyra 使用 |
| upgrade_yield | 已安装 Lyra · 建议由 Lyra 接管星环 |
| status_handed_off | 已移交 Lyra |
| custom_universal | 定制图同时适用于星澜与 Lyra（2990×284） |

---

## 10. 决策记录

| 决议 | 选择 |
|------|------|
| 产品关系 | 星澜为 Lyra 生态中的空闲壁纸工具 |
| 并排安装 | **允许**（过渡期） |
| 1.0 迁移 | 文件名兼容 + 引导；导出包 P0.5 |
| 丝滑底线 | U1–U6 可测；双装不恶性抢屏 |
| 代码 | 1.0 独立仓拷贝；契约优先于过早 monorepo |

---

## 11. 修订

| 日期 | 说明 |
|------|------|
| 2026-08-03 | 初版：子项目关系 + 丝滑升级契约 |
