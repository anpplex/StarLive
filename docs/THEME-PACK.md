# 星澜主题包格式（兑换 / 分发）

| 字段 | 值 |
|------|-----|
| 版本 | 1 |
| 日期 | 2026-08-03 |
| 消费端 | StarLive App `ThemePackInstaller` |
| 核销 | LicenseHub `POST /api/v1/starlive/exchange` |

相关：[SECURITY-REDEEM-CODES.md](./SECURITY-REDEEM-CODES.md) · 打包脚本 `scripts/make-theme-pack.sh`

---

## 1. 包形态

一个 **zip**，根目录（不要多余顶层文件夹）：

```text
pack_xxx.zip
├── catalog.json          # 必填
└── wallpaper.jpg         # 默认 1 张；路径以 catalog 为准
# 可选更多图：
# └── night.jpg
# └── day.jpg
```

- 推荐图片：**JPEG**，条带 **2990×284**（全宽 4032 条带亦可，App 会按右带裁切）
- **默认 1 张**；深/浅双图可选（catalog 两条）
- 暂不支持 GIF / 视频（产品演进备忘，非本格式）

---

## 2. catalog.json

```json
{
  "pack_id": "pack_demo_night",
  "title": "示范夜色",
  "version": 1,
  "wallpapers": [
    {
      "id": "main",
      "file": "wallpaper.jpg",
      "label": "夜色"
    }
  ]
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `pack_id` | 建议 | 与服务端 `starlive_packs.id` 一致 |
| `title` | 建议 | 安装成功提示与默认标签 |
| `version` | 可选 | 整数，默认 1 |
| `wallpapers` | **是** | 至少 1 条 |
| `wallpapers[].file` | **是** | zip 内相对路径 |
| `wallpapers[].label` | 可选 | 写入本地图库的显示名 |
| `wallpapers[].id` | 可选 | 文档用，App 可忽略 |

安装后每条壁纸进入 **本地导入库**（与相册导入同库），用户再点「应用」上屏。

---

## 3. 服务端登记（LicenseHub）

1. 将 zip 放到 VPS：`data/starlive-packs/<object_key>.zip`  
2. 表 `starlive_packs` 写入 `id / title / version / object_key / sha256 / status=active`  
3. Admin → **星澜兑换** → 批量生成码（`max_devices` 默认 1）  
4. 客户在星澜 App 输入码 → 核销绑定 device → 短时 URL 下载 → 安装  

本地 dev seed：`pack_demo_night` + `DEMO0001` / `DEMO0002`（仅开发库）。

---

## 4. 本地打包

```bash
# 单图
./scripts/make-theme-pack.sh \
  --id pack_custom_01 \
  --title "客户定制·夜景" \
  --image ./night.jpg \
  --out ./dist/pack_custom_01.zip

# 校验
unzip -l dist/pack_custom_01.zip
shasum -a 256 dist/pack_custom_01.zip
```

---

## 5. 非目标

| 不做 | 说明 |
|------|------|
| 带声视频 / GIF | 见产品演进讨论，暂缓 |
| 应用内商城目录 | 当前仅支持兑换码与私人定制 |
| 与 License 软件码混用 | 表与码空间隔离 |

---

## 6. 演进备忘（非承诺）

- 可选 `wallpapers[].role: "dark"|"light"` 与日夜开关联动  
- 包签名（防篡改）在 SHA-256 之外再加  
- 社区 GitHub Release 静图包（无核销）与付费兑换包分流  
