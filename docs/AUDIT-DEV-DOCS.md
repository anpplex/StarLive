# 星澜 StarLive — 开发文档缺口审计

| 字段 | 值 |
|------|-----|
| 日期 | 2026-08-03 |
| 远程 | https://github.com/anpplex/StarLive （**空库**） |
| 本地 | `/Users/anpple/Codex/StarLive`（**未 git init**，仅 `docs/` 产品四件套） |
| 原则 | **严格 GitHub Flow**（对齐 Lyra `docs/GIT_WORKFLOW.md` 精神） |

---

## 0. 现状快照

### 已有（产品/交互/技术 · 可进库）

| 文档 | 角色 |
|------|------|
| [FEATURE-ALIGNMENT-1.0.md](./FEATURE-ALIGNMENT-1.0.md) | 功能与方案 A |
| [INTERACTION-1.0.md](./INTERACTION-1.0.md) | 交互终稿 |
| [TECH-NOTES-1.0.md](./TECH-NOTES-1.0.md) | 工程映射与 P0 分期 |
| [LYRA-UPGRADE.md](./LYRA-UPGRADE.md) | **子项目 + 丝滑升级到 Lyra**（硬约束） |
| [AUDIT-GAPS-1.0.md](./AUDIT-GAPS-1.0.md) | 产品缺口跟踪（§1 已消化） |

### 缺失（相对「可协作 + 可 PR + 可 push 空库」）

| 类别 | 本地 | 远程 |
|------|------|------|
| Git 仓库 | ❌ 无 `.git` | ✅ 已建 / **0 commits** |
| 根 README / LICENSE / .gitignore | ❌ | ❌ |
| AGENTS.md / 分支工作流 | ❌ | ❌ |
| 工程 `android/` | ❌ | ❌ |
| CI | ❌ | ❌ |

**结论：** 产品规格已够 P0 设计；**开发协作与 Git 门面文档几乎为零**，且未完成「空库 bootstrap → main 首推」。

---

## 1. 必须补充（P0 · 首次 push 前）

严格 workflow 下，**没有这些不应开始 feature 分支写业务代码**。

| # | 文档/文件 | 用途 | 为何必须 |
|---|-----------|------|----------|
| 1 | **`README.md`**（根） | 项目名片、装机一句、链到 docs、免责 | 空库/开源门面；PR 读者入口 |
| 2 | **`LICENSE`** | Apache-2.0 全文 | TECH/交互已定协议；无 LICENSE 不算开源合规 |
| 3 | **`.gitignore`** | 忽略 build、.idea、APK、keystore、local.properties、OS 垃圾 | 防误提交密钥与巨物 |
| 4 | **`docs/GIT_WORKFLOW.md`** | 分支模型、commit、PR、与 remote 同步 | **严格 git workflow 的权威** |
| 5 | **`AGENTS.md`** | 目录边界、可改路径、并行 subagent 上限、禁止项 | 人/Agent 统一；避免改错树 |
| 6 | **`docs/README.md`** | 文档索引（产品 / 交互 / TECH / 工作流） | 避免 docs 沦为无序堆 |
| 7 | **首 commit 策略**（流程，非文件） | `main` 仅 docs+脚手架 → push `-u origin main` | 远程已建空库，需 **本地 init + remote + 保护 main** |

### `GIT_WORKFLOW.md` 必须写清的条款（清单）

从 Lyra 工作流裁剪到 StarLive，至少包含：

```text
1. 仓库根 = /Users/anpple/Codex/StarLive
2. 远程 origin = https://github.com/anpplex/StarLive.git
3. 默认分支 main（稳定、可构建后才合）
4. 分支：feature/* | fix/* | chore/* | docs/* | release/*
5. 循环：main pull → 建分支 → 改 → 构建 → commit → push → PR → 合 main
6. Conventional Commits：feat|fix|docs|refactor|chore|test|perf
7. scope 示例：display|wallpaper|boot|media|ui|docs|build
8. 禁止：force push main；提交 keystore/APK/逆向包；巨型无关格式化
9. PR 自检：assembleDebug、权限变更说明、实车是否验
10. Tag：v0.1.0 起与 versionName 对齐
11. 首推：允许 chore/docs bootstrap 直接上 main（空库例外），之后一律 PR
```

**空库例外（写进 workflow）：**

| 阶段 | 允许 |
|------|------|
| Bootstrap #1 | 本地 `git init`，`main` 上 `docs + README + LICENSE + gitignore + AGENTS`，`git push -u origin main` |
| Bootstrap #2（可选同 PR 或紧随） | `android/` 空壳可构建 |
| 之后 | **禁止**直推 main；一律 `feature/*` → PR |

---

## 2. 强烈建议（P0 写代码同期 / 首个 feature 前）

| # | 文档 | 用途 |
|---|------|------|
| 8 | **`docs/PROJECT_STRUCTURE.md`** | 目录树、`android/` 唯一产品工程、docs 角色 | 防止以后塞逆向/APK |
| 9 | **`docs/PRODUCT_BOUNDARIES.md`** | 可承诺/不可承诺（含 **升级 Lyra**） | 边界文档 |
| 10 | **`docs/ROADMAP.md`** | P0 Phase、P0.5 handoff、升级项 | 分支对齐 |
| 11 | **`docs/DECISIONS.md`** | 决议表（含 **子项目丝滑升级**） | 链 LYRA-UPGRADE |
| 11b | **Lyra 主仓 ROADMAP** | 从星澜导入 handoff | **双边**闭环 |
| 12 | **`CONTRIBUTING.md`**（根，可短） | 外链 `docs/GIT_WORKFLOW.md` + 构建命令 + 行为准则一句 | GitHub 协作者惯例；可与 workflow 合并但根目录有入口更好 |
| 13 | **`docs/PRIVACY.md`** | 权限、本地图、媒体状态用途、不联网默认 | 交互 About 已要求；发 APK 前要有正文 |

---

## 3. 发版 / 质量（P0.5 · 首包外发车友群前）

| # | 文档 | 用途 |
|---|------|------|
| 14 | **`docs/QA-MATRIX.md`** | 交互 §14 十三条 + 机型/系统列 | 提测与回归 |
| 15 | **`CHANGELOG.md`** | Keep a Changelog | tag 发版 |
| 16 | **`docs/releases/README.md`** 或 `v0.1.0.md` | 安装说明、已知问题、与 Lyra 共存 | 侧载包说明 |
| 17 | **`docs/CUSTOM-SOP.md`** | 定制接单话术与交付 | 接单前；非 git 阻塞但产品阻塞 |
| 18 | **GitHub**：Description、Topics、默认 branch、可选 branch protection | 远程库配置 | 空库补全 |

可选（不阻塞）：

| 文档 | 说明 |
|------|------|
| `SECURITY.md` | 漏洞反馈邮箱/微信 |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 自检勾选 |
| `.github/ISSUE_TEMPLATE/` | bug/feature |
| CI `/.github/workflows/android.yml` | PR 上 `assembleDebug` |

---

## 4. 明确不必现在写（防文档膨胀）

| 文档 | 原因 |
|------|------|
| 完整架构四层长文 | TECH + INTERACTION 已覆盖 1.0 |
| 凯迪/Lyra 再分析 | 已有对齐文 |
| 商业 License 体系 | 方案 A 无 |
| 多语言 i18n 规范 | 1.0 仅中文 |
| ADR 每条一文件 | 用单页 `DECISIONS.md` 即可 |

---

## 5. 推荐目录（bootstrap 后应长这样）

```text
StarLive/                          # = git root = 对齐 remote anpplex/StarLive
├── .gitignore
├── LICENSE                        # Apache-2.0
├── README.md
├── AGENTS.md
├── CONTRIBUTING.md                # 短，链 GIT_WORKFLOW
├── CHANGELOG.md                   # 可先 Unreleased
├── docs/
│   ├── README.md                  # 索引
│   ├── GIT_WORKFLOW.md            # ★ 工作流权威
│   ├── PROJECT_STRUCTURE.md
│   ├── PRODUCT_BOUNDARIES.md
│   ├── DECISIONS.md
│   ├── ROADMAP.md
│   ├── PRIVACY.md
│   ├── FEATURE-ALIGNMENT-1.0.md   # 已有
│   ├── INTERACTION-1.0.md         # 已有
│   ├── TECH-NOTES-1.0.md          # 已有
│   ├── AUDIT-GAPS-1.0.md          # 已有
│   ├── AUDIT-DEV-DOCS.md          # 本文
│   ├── QA-MATRIX.md               # 外发前
│   ├── CUSTOM-SOP.md              # 接单前
│   └── releases/
├── android/                       # 唯一产品工程（后续）
└── scripts/                       # 可选：install 到车机
```

**禁止进库：** keystore、`local.properties`、release APK、Lyra 整树、凯迪 APK、大图 bulk、`.idea` 可忽略。

---

## 6. 严格 Git 工作流：bootstrap 顺序（与文档绑定）

```text
【本地】
1. 补齐 P0 文档（§1）+ .gitignore + LICENSE
2. git init -b main
3. git add README LICENSE AGENTS CONTRIBUTING docs .gitignore
4. git commit -m "docs: bootstrap StarLive product specs and git workflow"
5. git remote add origin https://github.com/anpplex/StarLive.git
6. git push -u origin main

【之后开发】
7. git checkout -b chore/android-skeleton   # 或 feature/p0-phase0-shell
8. 加 android/ 空壳 → PR → 合 main
9. git checkout -b feature/p0-phase1-strip-display
10. … 按 ROADMAP/TECH Phase，一分支一类事
```

| 规则 | 说明 |
|------|------|
| main | 始终可说明「当前文档 + 可构建代码（有 android 后）」 |
| 分支名 | 对齐 ROADMAP：`feature/p0-phase2-import` |
| 文档变更 | 行为变了必须同 PR 改 docs（或 `docs:` 紧随 PR） |
| 大文件 | `.gitignore` 挡 APK；demo 图控制体积 |

---

## 7. 优先级总表

| 优先级 | 补什么 | 完成标志 |
|--------|--------|----------|
| **P0a 今日** | README · LICENSE · gitignore · GIT_WORKFLOW · AGENTS · docs/README · 本文 | `main` 已 push 到 anpplex/StarLive |
| **P0b 开工前** | PROJECT_STRUCTURE · PRODUCT_BOUNDARIES · DECISIONS · ROADMAP · CONTRIBUTING · PRIVACY | Agent/人知道改哪、承诺什么 |
| **P0c 有代码后** | CHANGELOG · PR template（可选）· CI（可选） | PR 可检 |
| **P0.5 外发** | QA-MATRIX · releases 说明 · CUSTOM-SOP · SECURITY（可选） | 车友群包 |

---

## 8. 与「只写产品文档」的差距

| 维度 | 现在 | 严格 workflow 目标 |
|------|------|-------------------|
| 远程 | 空 | main 有 bootstrap commit |
| 本地 | 无 git | 与 origin 同步 |
| 协作 | 无 AGENTS/分支约定 | 分支 + Conventional Commits + PR |
| 开源 | 无 LICENSE/README | Apache-2.0 可声明 |
| 开发 | TECH 有分期 | ROADMAP 可勾选 + 分支对齐 |

---

## 9. 建议下一步（执行）

1. **补齐 §1 P0a 文件**（可一次 commit bootstrap）  
2. **init + remote + push main**  
3. 再写 §2 P0b（可同一 commit 或 `docs/dev-meta` 第二 commit）  
4. 再 `chore/android-skeleton` PR  

若授权，可按 P0a→push 直接落盘生成上述文件。

---

## 10. 修订

| 日期 | 说明 |
|------|------|
| 2026-08-03 | 初版：对照空远程 + Lyra GIT_WORKFLOW + 本地仅产品 docs |
