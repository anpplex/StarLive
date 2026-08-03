# 私人定制履约 SOP（星澜）

| 字段 | 值 |
|------|-----|
| 适用 | 方案 A · 微信私人定制（非应用内支付） |
| 参考价 | ¥39 / ¥99（以实际报价为准） |
| 交付规格 | **2990×284** JPEG（推荐）；或 4032×284 全条由 App 裁右带 |

相关：[THEME-PACK.md](./THEME-PACK.md) · [SECURITY-REDEEM-CODES.md](./SECURITY-REDEEM-CODES.md) · LicenseHub Admin

---

## 1. 接单话术（简）

```text
星澜壁纸定制：做成阿维塔星环空闲条（2990×284）。
· 单图 ¥__ / 精修 ¥__
· 交付：兑换码 1 个（一码一车机）或 直发 jpg
· 修改：初稿 1 次微调；大改另议
· 请提供：参考图 / 色调 / 是否要深浅两版
```

**拒单：** 侵权 IP、血腥暴力、无法裁成条带的极端构图、要求带声视频（暂不做）。

---

## 2. 制作清单

1. 出图 **2990×284**（sRGB，JPEG 质量 ≥90）  
2. （可选）深/浅各一张  
3. 本地预览：`make-theme-pack.sh` 或装进车机导入确认  
4. 左缘可交给 App softener，不必客户自 fade  

```bash
./scripts/make-theme-pack.sh \
  --id pack_client_YYYYMMDD \
  --title "客户昵称·定制" \
  --image ./out.jpg \
  --label "定制" \
  --out ./dist/pack_client_YYYYMMDD.zip
```

---

## 3. 上架兑换（推荐）

1. 上传 zip → 服务器容器 `/data/starlive-packs/`  
2. Admin → **星澜兑换** → **登记主题包**（自动 SHA）  
3. 批量生成 **1** 个码 · `max_devices=1` · 备注订单号  
4. 复制码发给客户；指导：星澜 → 兑换主题 → 应用上屏  

**不要**把软件 License（LUMINA1）和主题兑换码混发。

### 直发图（免兑换）

- 发 `starlive_wallpaper.jpg`，说明放 Download 后点「导入」  
- 适合内测 / 熟人；**无防扩散**  

### 上传脚本

```bash
export LICENSEHUB_VPS=root@98.126.31.173
./scripts/ops-upload-pack.sh ./dist/pack_client.zip
# 然后 Admin 登记 + 发码
```

---

## 4. 修改与售后

| 情况 | 处理 |
|------|------|
| 色偏 / 裁切微调 | 改图 → 同 pack 升 version 或新 pack_id → 新码或同设备重兑（视策略） |
| 换机 | 新码或 Admin 作废旧码后补发（默认一码一设备） |
| 码泄露 | Admin **作废** → 发新码 |
| 退款 | 作废码；已下载文件无法远程删除 |

---

## 5. 交付 checklist

- [ ] 尺寸 2990×284（或已测 4032 裁切）  
- [ ] zip + catalog.json 正确  
- [ ] 生产 SHA 登记成功  
- [ ] 兑换码仅发客户一人  
- [ ] 客户完成上屏截图或口述确认  
- [ ] 订单备注写入 Admin note  

---

## 6. 不承诺

- 系统 OTA 后星环 Display 策略变化导致失效  
- 与官方壁纸商店互通  
- 一码多车（除非明确卖多设备授权）  
