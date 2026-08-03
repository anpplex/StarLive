# 星澜主题兑换码 · 安全架构与审计

| 字段 | 值 |
|------|-----|
| 日期 | 2026-08-03 |
| 范围 | 主题包兑换码（规划）；对照凯迪壁纸兑换 + LicenseHub 一码一车 |
| 目标 | 明确「一码多人用」是否会发生、如何防、与现网 VPS 如何落地 |

相关：[凯迪 PAYMENT 流](../../Lyra/research/apks/kaidi-wallpaper-1.0.20/PAYMENT-CONTROL-FLOW.md) · LicenseHub [PRODUCT.md](../../LicenseHub/docs/PRODUCT.md)

---

## 0. 结论摘要

| 问题 | 答案 |
|------|------|
| 凯迪会不会「一码多人兑」？ | **取决于服务端核销是否原子、是否绑 device**；客户端只 `GET /exchange/{6位}`，**若服务端不写「已用」或只返回资源元数据，则极易被多人用** |
| 纯签名码 / 静态码无服务端？ | **一定会出现一码多人用**（转发即共享） |
| 我们要不要允许一码多人？ | **默认不允许**（一码一设备；同设备可幂等重兑） |
| LicenseHub VPS 能否扛安全模型？ | **核销 API 够**；**包文件必须短时签名 URL + 外置存储**，防链传播 |

**写死原则（对齐 LicenseHub，严于凯迪客户端可见部分）：**

1. **授权真相在服务端**（核销表 + 下载授权），不信任 App 本地「已兑换」标记。  
2. **一码一设备**（`device_id` 绑定）；同设备重复兑换 = 幂等成功，不二次消耗。  
3. **码不可枚举**（足够熵 + 限流 + 统一错误）。  
4. **包不可裸链**（短时签名 URL 或需 token 的下载）。  
5. **并发核销原子**（DB 事务 / `UPDATE … WHERE status='unused'`）。

---

## 1. 威胁模型：什么叫「一码多人兑换」

```text
攻击者/用户 A 合法得到码 C
  → 把 C 发到群
  → B、C、D 各自在 App 输入 C
  → 若都成功解锁同一主题包 = 一码多人用（你亏钱）
```

| 变种 | 说明 |
|------|------|
| **T1 转发共享** | 多人先后兑换同一码 |
| **T2 并发抢兑** | 两人同时 POST，双双成功（竞态） |
| **T3 枚举码** | 6 位数字穷举撞库 |
| **T4 下载链泄露** | 兑换一次拿到永久 zip URL，群内转发包 |
| **T5 清数据重兑** | 同人清 App 变新 device，再兑一次（若未绑死） |
| **T6 换机** | 正当需求 vs 滥用，需客服策略 |
| **T7 中间人** | HTTP 明文窃听码与 URL |
| **T8 管理端拖库** | 未使用码库泄露 |

---

## 2. 对照：凯迪壁纸

### 2.1 客户端可见行为（APK 分析）

| 项 | 凯迪 1.0.20 |
|----|-------------|
| 兑换 | `GET /portal/wallpaper/exchange/{sixDigits}` |
| 成功结果 | 归一成一条壁纸对象，再走下载/设置 |
| 设备身份 | `cadillac-{timestamp}-{random}` 存本地 → `device-login` → `X-APP-TOKEN` |
| 付费壁纸 | 下载接口服务端校验「请先购买」（已实测） |
| 码形态 | **6 位数字** → 空间仅 100 万，**可枚举** |

### 2.2 安全含义（推断 + 已知）

| 点 | 评估 |
|----|------|
| 付费下载 | **服务端是门**，前端 `purchased` 不可信（文档已强调） |
| 兑换接口 | 客户端**未见**「提交 deviceId 核销」的完整契约暴露；若服务端仅「码→壁纸元数据」且不落库 used，则 **T1 高危** |
| 6 位数字 | **T3 高危**（需服务端强限流/锁定） |
| deviceId 可丢 | 清数据 = 新设备，**T5/T6 依赖运营** |
| 下载 jobNo | 近似 bearer，短时/绑定不够则 **T4** |

**对比结论：** 凯迪在 **付费下载** 上相对认真；**兑换码** 在公开客户端信息下 **不能默认认为已防多人用**。我们设计时 **按「最坏：无原子核销」来防**，而不是假设凯迪已经完美。

---

## 3. 对照：LicenseHub（应对齐的标杆）

| 原则 | LicenseHub | 主题兑换应对齐 |
|------|------------|----------------|
| 先身份再权益 | 先 bind 再买/发码 | 兑换请求 **必带 device_id** |
| 一码一车 | payload.sn = bind | **一码一 device**（见策略） |
| 真相在中台 | 私钥/订单在服务端 | 核销表在服务端 |
| 限流 | product+bind 拉码限流 | code 前缀 + IP + device 限流 |
| 禁止 | 「不绑设备的万能码」写进产品原则 | **禁止默认发万能无限码** |

主题包与软件授权差异：主题是 **内容权益**，可允许「同设备重装幂等」；软件是 **整端能力**。二者表结构分开，勿混 `LUMINA1` 码空间。

---

## 4. 目标安全属性（STARLIVE-REDEEM）

| ID | 属性 | 要求 |
|----|------|------|
| S1 | **单次消耗** | 全局最多 1 个成功核销绑定（默认策略） |
| S2 | **设备绑定** | 成功后 `bound_device_id` 固定；他设备兑换 → `CODE_USED` |
| S3 | **同设备幂等** | 同码 + 同 device 再请求 → 200 + 同一 pack 下载权，**不报错吓人** |
| S4 | **原子核销** | 并发双请求仅 1 成功 |
| S5 | **抗枚举** | 码熵 ≥ 40 bit 有效；失败统一文案；限流 |
| S6 | **抗盗链** | 下载 URL ≤ 10–15 分钟；绑定 code 会话或 device token |
| S7 | **传输** | 仅 HTTPS |
| S8 | **可审计** | 谁、何时、何 device、何 IP、结果码 |
| S9 | **可客服** | 换机：Admin 解绑/重绑（人工） |

---

## 5. 推荐方案：服务端核销（LicenseHub 同机）

### 5.1 数据模型

```text
starlive_packs(
  id TEXT PK,           -- e.g. pack_spring_2026
  title TEXT,
  version INT,
  object_key TEXT,      -- OSS key，非公网永久 URL
  sha256 TEXT,          -- 包完整性
  status TEXT           -- active|disabled
)

starlive_redeem_codes(
  code TEXT PK,         -- 见 §5.2 码格式
  pack_id TEXT NOT NULL,
  status TEXT NOT NULL, -- unused | bound | revoked
  bound_device_id TEXT, -- 核销后写入
  bound_at TEXT,
  max_devices INT DEFAULT 1,  -- 默认 1；活动码可 1
  note TEXT,            -- 渠道/订单备注
  created_at TEXT,
  expires_at TEXT       -- 可选：码本身过期
)

starlive_redeem_events(  -- 审计，只追加
  id, code, device_id, ip, ua, result, created_at
)

-- 可选：多设备策略时
starlive_code_devices(
  code, device_id, first_seen_at, last_seen_at,
  PRIMARY KEY(code, device_id)
)
```

### 5.2 码格式（防 T3）

| 方案 | 例 | 熵 | 建议 |
|------|----|-----|------|
| 6 位数字（凯迪） | `482917` | ~20 bit | **不采用**作唯一防线 |
| 8–10 位 Crockford Base32 | `A1B2-C3D4` | ~40–50 bit | **推荐零售** |
| 16+ 字节随机 hex | `a1b2…` | 高 | 渠道大批量 |

生成：CSPRNG；入库前 **规范化**（去空格、统一大写）。  
展示可分组，存储无连字符。

### 5.3 兑换 API（安全契约）

```http
POST /api/v1/starlive/exchange
Content-Type: application/json

{
  "code": "A1B2C3D4",
  "device_id": "starlive-aid-xxxxxxxx",
  "app_version": "0.1.4-lib"
}
```

**成功 200**

```json
{
  "ok": true,
  "pack_id": "pack_spring_2026",
  "title": "春日城市",
  "version": 3,
  "sha256": "...",
  "download_url": "https://oss.../..?sign=...&exp=...",
  "download_expires_at": "2026-08-03T12:15:00Z",
  "already_bound": false
}
```

**失败（统一对外文案，内部细分日志）**

| HTTP | code | 对外 msg | 含义 |
|------|------|----------|------|
| 400 | `INVALID` | 兑换码无效 | 不存在/格式错 |
| 409 | `USED` | 兑换码已被使用 | 绑了别的设备 |
| 410 | `REVOKED` / `EXPIRED` | 兑换码不可用 | 作废或过期 |
| 429 | `RATE` | 请求过于频繁 | 限流 |
| 503 | — | 服务暂不可用 | 降级 |

**禁止**返回：「还剩 N 次」「差一位」「属于某某渠道」等利于枚举的信息。

### 5.4 核销伪代码（防 T1/T2）

```sql
-- 单设备策略 max_devices=1
BEGIN;
SELECT status, bound_device_id, pack_id FROM starlive_redeem_codes
  WHERE code = ? FOR UPDATE;  -- SQLite: 立即事务内 UPDATE 条件即可

-- 情况 A: unused
UPDATE starlive_redeem_codes
  SET status='bound', bound_device_id=?, bound_at=datetime('now')
  WHERE code=? AND status='unused';
-- changes()==1 → 成功；==0 → 并发输掉，再 SELECT 判断是否同设备

-- 情况 B: bound && bound_device_id == ?
-- → 幂等成功，重新签发 download_url

-- 情况 C: bound && bound_device_id != ?
-- → USED

COMMIT;
INSERT INTO starlive_redeem_events(...);
```

SQLite 在 LicenseHub 上可接受：兑换 QPS 低；用 **单连接写** 或短事务即可压 T2。

### 5.5 下载授权（防 T4）

| 做法 | 说明 |
|------|------|
| **推荐** | OSS/R2 **预签名 GET**，`exp` 5–15 分钟；与 code 无关可再传 |
| 可选加强 | URL 内嵌 `jti`，下载网关验 jti 一次性（更重） |
| **禁止** | 永久公开 `https://cdn/pack.zip` 写进 App |
| 客户端 | 下载后校验 `sha256`；失败可同设备再调 exchange 取新 URL |

即使码被转发出去，**没有二次核销权的人拿不到新签名链**；已泄露的短链也会过期。

### 5.6 设备 ID（防 T5 滥用 vs 正当重装）

| 规则 | 说明 |
|------|------|
| 生成 | `starlive-` + `Settings.Secure.ANDROID_ID`（稳定于单机用户） |
| 存储 | 本地 SharedPreferences；**不要**用 `random+timestamp` 当主 ID（凯迪痛点） |
| 清数据 | ANDROID_ID 通常仍在 → 同设备幂等仍成功 |
| 换机/双清极端 | 视为新设备 → 需 Admin **换绑**（对齐 LicenseHub 换绑流程） |
| 模拟器 | 可拒绝或单独策略 |

### 5.7 限流（防 T3）

| 维度 | 建议 |
|------|------|
| IP | 10 次 / 分钟失败 |
| device_id | 20 次 / 小时失败 |
| 全局 code 不存在 | 指数退避 / 临时封禁 IP |
| 成功兑换 | 同 device 可略宽（幂等） |

对齐 LicenseHub 已有 rate-limit 中间件思路。

---

## 6. 策略矩阵（产品可选）

| 策略 | 行为 | 何时用 |
|------|------|--------|
| **P-默认：一码一设备** | 上表 S1–S3 | 零售主题包 |
| **P-家庭：一码最多 N 设备** | `max_devices=2~3` + `starlive_code_devices` | 明确卖家庭版时 |
| **P-活动：一码一用任意人** | 先到先得，不绑设备 | 抽奖；**仍要原子 unused→used** |
| **P-万能测试码** | Admin 专用、可吊销、强审计 | 内测 only，不上生产零售 |

**默认实现只做 P-默认**；N 设备作为显式 SKU 字段，避免 silently 多人用。

---

## 7. 不安全方案为何会「多人兑」

| 方案 | 一码多人？ | 原因 |
|------|------------|------|
| 群发同一网盘链接 | **是** | 无核销 |
| 纯 HMAC 签名码、App 本地验 | **是** | 无全局 used 状态 |
| 服务端返回包但 `UPDATE` 非条件写 | **可能** | 竞态 T2 |
| 6 位数字 + 无限流 | **易被撞** | T3 |
| 永久 download_url | **等于发盘** | T4，比多人兑更糟 |

---

## 8. App（星澜）安全职责

| 做 | 不做 |
|----|------|
| 生成/持久化稳定 device_id | 不把「已兑换列表」当唯一授权 |
| HTTPS only | 不缓存永久下载链 |
| 下载后 sha256 | 不在日志打印完整码 |
| 失败展示统一文案 | 不根据错误差异提示「码差一位」 |
| 本地记录 pack_id 已装版本 | 不信任本地记录绕过服务端再下载（再下仍走 exchange 幂等） |

开源注意：API Base URL 可公开；**Admin 发码、OSS 密钥、HMAC 私钥** 永不进公开仓库。

---

## 9. Admin / 运营安全

| 能力 | 要求 |
|------|------|
| 批量生成码 | 仅 Admin 登录；输出一次展示/下载，库内可只存 hash（可选） |
| 吊销 | `status=revoked`；已 bound 是否作废下载权 → 产品决策（建议吊销后拒绝新 URL） |
| 换绑 | 填 code + 新 device_id；写审计 |
| 导出未使用码 | 权限最高；操作进 audit log |
| 与支付 | 微信到账备注订单号 → Admin 勾选 pack 生成码（可人工） |

码存储可选：

- **明文 code**（运维简单，拖库全丢）  
- **hash(code)**（库泄露难打明文，客服难查）  

起步可用明文 + 盘权限 + 备份加密；量上来再 hash。

---

## 10. 与 LicenseHub 同 VPS 的安全隔离

| 项 | 建议 |
|----|------|
| 路由前缀 | `/api/v1/starlive/*` 与 `/api/v1/license*` 分离 |
| 表前缀 | `starlive_*`，不碰 licenses 表 |
| 密钥 | 下载签名密钥 ≠ license RSA 私钥 |
| 限流 | 独立 bucket |
| 开关 | `STARLIVE_REDEEM_ENABLED=0` 可熔断 |
| 日志 | 兑换事件不与 license 签发混在难读的一锅里 |

这样兑换被刷时，尽量不拖垮买 Lyra 授权。

---

## 11. 审计清单（实现前/上线前）

### 必须通过

- [ ] 两设备同时兑同一码，仅一设备成功（自动化测）  
- [ ] 同设备兑两次，均成功且 `already_bound=true` 第二次  
- [ ] 错误码不区分「不存在 / 格式错」的可枚举差异（或均 INVALID）  
- [ ] 下载 URL 过期后 403/失败；同设备可重新 exchange 取新 URL  
- [ ] zip sha256 不符则安装中止  
- [ ] 无限流脚本 1 分钟内被 429  
- [ ] HTTPS only；无混合内容  
- [ ] Admin 发码需登录；未登录 401  

### 对照凯迪的改进点（我们应更严）

| 凯迪风险 | 我们的控制 |
|----------|------------|
| 6 位数字 | ≥8 位高熵码 |
| 兑换是否绑设备不明 | **强制 device_id + bound** |
| jobNo 类下载 | **短时签名 URL** |
| device 随机易丢 | **ANDROID_ID 稳定 ID** |
| 前端状态不可信 | **服务端核销唯一真相** |

---

## 12. 分阶段落地（安全不缩水）

| 阶段 | 做 | 安全底线 |
|------|----|----------|
| **M0 人工** | 微信发卡 + 网盘 | 接受共享风险；不在 App 宣称「一码一车」 |
| **M1 API** | LicenseHub 核销 + OSS 签名下发 | **S1–S7 全开** |
| **M2 运营** | Admin 批量码、换绑、报表 | S8–S9 |
| **M3** | 家庭多设备 SKU | 显式 max_devices |

**禁止**跳过 M1 直接做「App 本地验签当完成品」。

---

## 13. 决议（建议写入 DECISIONS）

| ID | 决议 |
|----|------|
| R1 | 主题兑换默认 **一码一设备**，同设备幂等 |
| R2 | 核销与下载授权 **必须在服务端** |
| R3 | 码熵拒绝纯 6 位数字作唯一方案 |
| R4 | 包文件 **短时 URL + 外置存储** |
| R5 | 与 LicenseHub 同机可，表/密钥/限流隔离 |
| R6 | 换机仅 Admin 换绑，不自动放行第二设备 |
| R7 | 单码可配置 **限时兑换** 与 **限次数/限设备**；字段见 §15 |

---

## 15. 限时兑换 × 限次数兑换（可按码配置）

### 15.1 结论

| 能力 | 能否 | 说明 |
|------|------|------|
| **限时兑换** | ✅ | 码在 `redeem_before` 之后不可**首次**核销；已绑定是否续下包装另定 |
| **限次数兑换** | ✅ | 用 `max_redemptions` / `max_devices` 控制成功核销次数或设备数 |
| **同一码同时限时+限次** | ✅ | 两条件 **都满足** 才允许新核销 |
| **每码不同策略** | ✅ | 字段落在 `starlive_redeem_codes` 行上，不靠全局写死 |

### 15.2 两个「时间」不要混

| 字段 | 含义 | 到期后 |
|------|------|--------|
| **`redeem_before`**（限时兑换） | 最晚 **完成首次/新设备核销** 的时刻 | 未核销 → `EXPIRED`；**已绑定设备**默认仍可幂等取下载链（可选：也切断，见策略） |
| **`entitlement_until`**（内容有效期，可选） | 权益可用到何时 | 已兑设备再要下载 URL → `ENTITLEMENT_EXPIRED`；本地已下文件管不了 |

零售常见：只设 **`redeem_before`（活动 7 天内兑完）**，兑完后包可长期用。  
订阅感内容：再加 **`entitlement_until`**（少见，壁纸包一般不必）。

### 15.3 两个「次数」不要混

| 字段 | 含义 | 典型值 |
|------|------|--------|
| **`max_devices`** | 最多绑定几个 **不同 device_id** | 默认 **1**（一码一车）；家庭版 **2～3** |
| **`max_redemptions`** | 最多成功 **核销事务** 次数（含首次+换绑？） | 通常 **= max_devices** 或单独限制「可换绑次数」 |

推荐实现语义（清晰好讲）：

```text
成功条件（新 device 首次绑定）：
  now < redeem_before（若设置）
  AND 当前已绑定设备数 < max_devices
  AND status ∈ {unused, bound} 且未 revoked

同 device 再次 exchange：
  若已在 bound 列表 → 幂等成功（不增加设备数）
  若 entitlement_until 过期 → 拒绝新下载链
```

| 产品话术 | 配置示例 |
|----------|----------|
| 一码一车，永久包 | `max_devices=1`，无 redeem_before |
| 活动码 72 小时内兑，一车 | `max_devices=1`，`redeem_before=下单+72h` |
| 家庭码两台车 | `max_devices=2`，无时限 |
| 渠道码 10 次（10 台车） | `max_devices=10`，建议仍要绑 device 防刷 |
| 测试码可吊销 | `status=revoked` 优先于一切 |

**不推荐：**「不绑设备、只 max_redemptions=10」的纯次数码——易变成 **10 次匿名下载**，码进群仍被抢光，且难追责。次数应落在 **设备槽位** 上。

### 15.4 表字段（在 §5.1 上扩展）

```text
starlive_redeem_codes(
  code TEXT PK,
  pack_id TEXT NOT NULL,
  status TEXT NOT NULL,          -- unused | bound | exhausted | revoked
  max_devices INT NOT NULL DEFAULT 1,
  redeem_before TEXT,             -- ISO8601，NULL=不限制兑换截止
  entitlement_until TEXT,         -- ISO8601，NULL=绑定后下载不限期
  note TEXT,
  created_at TEXT
)

starlive_code_devices(
  code TEXT NOT NULL,
  device_id TEXT NOT NULL,
  bound_at TEXT NOT NULL,
  last_redeem_at TEXT,
  PRIMARY KEY (code, device_id)
)
```

- `unused`：尚无任何 device  
- `bound`：已有 ≥1 device，且未达 max  
- `exhausted`：设备数已达 max_devices（可选冗余状态，便于查询）  
- `revoked`：运营吊销，优先拒绝  

`status=bound` 且 `count(devices) < max_devices` 时，**新 device 仍可兑**（家庭码）。  
达到上限后新 device → `USED` / `EXHAUSTED`。

### 15.5 判定顺序（实现）

```text
1. 格式非法 → INVALID
2. 码不存在 → INVALID（与格式错对外同文案）
3. status == revoked → REVOKED
4. 若 redeem_before 且 now > redeem_before：
     - 本 device 已在 code_devices → 走幂等（若允许过期后重下）
     - 否则 → EXPIRED（限时未兑）
5. 本 device 已绑定 → 检查 entitlement_until → 签发 download_url
6. 本 device 未绑定：
     - count(devices) >= max_devices → USED/EXHAUSTED
     - 否则 INSERT device，必要时更新 status，签发 URL
7. 写 redeem_events 审计
```

### 15.6 并发

新设备占用槽位：

```sql
-- 伪代码：在事务内
n = SELECT COUNT(*) FROM starlive_code_devices WHERE code=?
IF n >= max_devices THEN fail
INSERT OR IGNORE INTO starlive_code_devices ...
-- 或：仅当 count < max 时 insert，用唯一约束防双插
```

同设备双请求：`PRIMARY KEY (code, device_id)` + 幂等返回。

### 15.7 对外错误码（补充）

| code | 用户文案建议 |
|------|----------------|
| `EXPIRED` | 兑换码已过期 |
| `EXHAUSTED` / `USED` | 兑换码已达使用上限 / 已被使用 |
| `ENTITLEMENT_EXPIRED` | 权益已到期（若启用内容有效期） |

限时与限次可同时触发时，优先级：`revoked` > `expired(未绑定)` > `exhausted` > 其它。

### 15.8 和「一码多人用」的关系

| 配置 | 是否多人用 |
|------|------------|
| max_devices=1 | **否**（第二人失败） |
| max_devices=3 | **是，但上限 3 台设备**（产品明示家庭/多车） |
| 仅 redeem_before、max_devices=1 | 时限内仍只能 1 台 |
| 无核销、只发网盘 | 无限多人（禁止当完成品） |

**限时解决的是「活动窗口」，不解决「转发共享」；限设备数才解决共享。**

### 15.9 Admin 发码时建议填的字段

| 字段 | 必填 | 默认 |
|------|------|------|
| pack_id | 是 | — |
| max_devices | 是 | 1 |
| redeem_before | 否 | NULL |
| entitlement_until | 否 | NULL |
| note | 否 | 渠道/订单号 |
| 生成数量 | 是 | 批量 |

UI 文案示例：「该码须在 2026-08-10 前兑换，最多 1 台车机。」

---

## 16. 修订

| 日期 | 说明 |
|------|------|
| 2026-08-03 | 初版安全审计；对照凯迪兑换与 LicenseHub 一码一车 |
| 2026-08-03 | §15 限时兑换 + 限次数/限设备；决议 R7 |
