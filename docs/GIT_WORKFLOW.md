# StarLive Git Workflow

后续所有开发按本文执行。目标：可回滚、可评审、与 [anpplex/StarLive](https://github.com/anpplex/StarLive) 同步，避免脏提交。

---

## 1. 仓库根与远程

| 项 | 值 |
|----|-----|
| 本地根 | `/Users/anpple/Codex/StarLive` |
| 远程 | `https://github.com/anpplex/StarLive.git` |
| 默认分支 | **`main`** |

### 跟踪内容

- `docs/`
- `android/`（产品工程，建成后）
- 小型 `scripts/`（可选）
- 根：`README.md` · `AGENTS.md` · `CONTRIBUTING.md` · `LICENSE` · `.gitignore` · `CHANGELOG.md`

### 禁止跟踪

- `*.apk` / keystore / `local.properties` / `.env`
- 逆向包、jadx 树、凯迪 APK、Lyra 整仓拷贝
- `build/` · `.gradle/`

见根目录 `.gitignore`。

---

## 2. 分支模型（GitHub Flow）

| 分支 | 用途 |
|------|------|
| `main` | 稳定；文档 +（有工程后）可构建；打 tag 发版 |
| `feature/<topic>` | 功能：如 `feature/p0-phase1-strip` |
| `fix/<topic>` | 缺陷 |
| `chore/<topic>` | 工程：脚手架、依赖、gitignore |
| `docs/<topic>` | 仅文档 |

命名尽量对齐 `docs/ROADMAP.md` / TECH Phase：

```text
chore/android-skeleton
feature/p0-phase1-strip-display
feature/p0-phase2-import
feature/p0-phase3-boot-media
docs/privacy
```

---

## 3. 日常循环

```text
1. git checkout main && git pull
2. git checkout -b feature/<topic>
3. 只改 android/ + 必要 docs
4. cd android && ./gradlew :app:assembleDebug   # 有工程后
5. 车机验证（若有）+ 更新 ROADMAP 勾选
6. git add -p && git commit
7. git push -u origin HEAD
8. 开 PR → 自检 → 合并 main
```

### Conventional Commits

```text
<type>(<scope>): <summary>

type:  feat | fix | docs | refactor | chore | test | perf
scope: display | wallpaper | boot | media | ui | upgrade | docs | build
```

示例：

```text
docs: bootstrap product specs and git workflow
chore(build): android empty project skeleton
feat(display): launch cluster strip wallpaper activity
fix(boot): debounce recover when idle prefer off
```

**一条提交一类事**；禁止巨型 `fix everything`。

---

## 4. 空库 Bootstrap 例外

远程初始为空时，**允许一次直推 `main`**：

```text
git init -b main
git add README.md LICENSE AGENTS.md CONTRIBUTING.md .gitignore docs CHANGELOG.md
git commit -m "docs: bootstrap StarLive product specs and git workflow"
git remote add origin https://github.com/anpplex/StarLive.git
git push -u origin main
```

| 之后 | 规则 |
|------|------|
| 业务 / 工程代码 | **禁止**再直推 main；一律分支 + PR |
| 热修文档小改 | 仍优先 PR；紧急可 main 但需说明 |

---

## 5. PR 自检清单

合并前：

- [ ] 有 `android/` 时：`./gradlew :app:assembleDebug` 成功  
- [ ] 未提交 APK、密钥、`local.properties`  
- [ ] 行为变更已更新 `docs/`（ROADMAP / INTERACTION 修订记录如需）  
- [ ] Manifest 变更：说明权限/组件原因  
- [ ] 显示/播控/开机：注明是否实车已验  
- [ ] **未破坏** 2990×284 / handoff / `lyra_wallpaper` 兼容（[LYRA-UPGRADE](./LYRA-UPGRADE.md)）  
- [ ] 无无关格式化大爆炸  

---

## 6. 版本与发版

| 字段 | 规则 |
|------|------|
| `versionName` | 语义化；首版目标 `0.1.0` |
| `versionCode` | 单调递增整数 |

```bash
git tag -a v0.1.0 -m "StarLive 0.1.0"
git push origin v0.1.0
```

APK **不进 Git**；本地 `releases/` 可忽略二进制，仅跟踪说明 md。

---

## 7. 与路线图联动

| 动作 | 文档 |
|------|------|
| 完成 Phase | `docs/ROADMAP.md` 勾选 + 日期 |
| 改产品边界 | `PRODUCT_BOUNDARIES` + `DECISIONS` |
| 升级契约变更 | 必须改 `LYRA-UPGRADE.md` 并评估 Lyra 主仓 |

---

## 8. 默认节奏

- **Keep dev**：无阻塞时持续按 ROADMAP 开发，不逐步请示。
- **停下问人**：仅 bug 歧义，或重要决策（边界 / 升级契约 / 商业模式 / 包名变更等）。

## 9. 修订

| 日期 | 说明 |
|------|------|
| 2026-08-03 | 初版；空库 bootstrap 例外 + GitHub Flow |
| 2026-08-03 | Standing order：default keep dev |
