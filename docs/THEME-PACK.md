# 主题包格式

| 项目 | 值 |
|------|-----|
| 格式版本 | 1 |
| 安装端 | StarLive `ThemePackInstaller` |
| 打包 | `scripts/make-theme-pack.sh` |

## 包结构

根目录 zip（不要多余顶层文件夹）：

```text
pack_xxx.zip
├── catalog.json          # 必填
└── wallpaper.jpg         # 路径以 catalog 为准
```

- 推荐 **JPEG**，**2990×284**；全宽 4032 条带亦可（应用裁取右侧）  
- 支持 catalog 中多条壁纸；暂不支持 GIF / 视频  

## catalog.json

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

| 字段 | 说明 |
|------|------|
| `pack_id` | 与服务端主题包 id 一致 |
| `title` | 展示标题 |
| `version` | 整数，默认 1 |
| `wallpapers[].file` | zip 内相对路径（必填） |
| `wallpapers[].label` | 图库显示名 |

安装后进入本地图库，由用户点「应用到星环」。

## 兑换安全（摘要）

- 一码一设备；核销与下载授权在服务端完成  
- 下载使用短时 URL，避免永久裸链传播  
- 主题兑换码与软件 License 码隔离  

## 本地打包

```bash
./scripts/make-theme-pack.sh \
  --id pack_custom_01 \
  --title "客户定制" \
  --image ./night.jpg \
  --out ./dist/pack_custom_01.zip

unzip -l dist/pack_custom_01.zip
shasum -a 256 dist/pack_custom_01.zip
```
