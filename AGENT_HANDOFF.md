# AGENT_HANDOFF — 篮球数据统计平台（hoops）

> **本文件用途**：把当前进度完整交给下一个 Agent，使其无需重新分析整个项目即可继续。  
> **生成时间**：2026-10-01  
> **仓库路径**：`C:\Users\朱毅城\Desktop\big-event-fullstack`  
> **当前分支**：`feature/basketball`  
> **工作区状态**：干净（无未提交改动，仅 `AGENT_HANDOFF.md` 未跟踪）  
> **远端**：`https://gitee.com/PaulGeorge8/big-event-fullstack.git`

**阅读顺序建议**：先看第 3 节（环境）和第 11 节（下一步），再按需查其余章节。

---

## 1. 项目目标

### 背景

项目起点是**跟着网课（黑马程序员「大事件」）敲出来的通用后台管理 CRUD 系统**。用户已明确承认这一点，并决定在**保留可用骨架**的前提下重构成有个人风格的项目。

### 目标

| 项 | 内容 |
|---|---|
| 项目定位 | **篮球数据统计平台** —— 球队/球员/赛季/比赛管理 + 单场数据导入 + 进阶指标计算 + 排行榜 + 投篮热图 |
| 目标岗位 | **全栈工程师**（暑期实习） |
| 时间预算 | **1 个月（约 4 周 / 80~100 小时）** |
| 完整方案 | 见仓库根目录 **`DESIGN.md`**（含 6 张表 DDL、数据方案、三个技术亮点、四周排期、验收标准） |
| 数据来源决策 | **CSV 种子数据 + 批量导入为主链路**；公开 API 同步为"有时间再做"的增强项 |

### 三个技术亮点（用户已确认）

1. **进阶数据计算引擎 + 单元测试**（eFG% / TS% / 效率值等 8 项指标）
2. **排行榜 + Redis 缓存 + 性能对比**（量化数字如 `320ms → 12ms`）
3. **投篮热图 Shot Chart**（后端按 8 区域聚合命中率 + 前端 ECharts 半场图）

### 非目标

- ❌ PER（球员效率值）
- ❌ 实时比赛直播数据接入
- ❌ 移动端 App / 小程序
- ❌ 视频、社交、评论、点赞
- ❌ 微服务拆分
- ❌ 全量 TypeScript 迁移

---

## 2. 当前项目结构

```
big-event-fullstack/
├── README.md                           已重写（篮球数据平台定位）
├── DESIGN.md                           重构设计方案
├── TEST-CHECKLIST.md                 人工测试清单
├── AGENT_HANDOFF.md                  本文件（未跟踪，不提交）
├── .gitignore                          已忽略 target/ .idea/ node_modules/ dist/ uploads/
├── big-event/                          后端（Spring Boot）
│   ├── pom.xml
│   ├── test.http                       IDEA HTTP Client 用例
│   ├── test.jpg
│   └── src/main/java/com/zhuyicheng/hoops/
│       ├── HoopsApplication.java
│       ├── config/WebConfig.java
│       ├── controller/                 ArticleController / CategoryController / FileUploadController / UserController
│       │                              + 篮球：SeasonController / TeamController / PlayerController / GameController
│       ├── service/                    ArticleService / CategoryService / UserService + 篮球 4 Service
│       ├── service/impl/
│       ├── mapper/                     ArticleMapper / CategoryMapper / UserMapper + 篮球 4 Mapper
│       ├── pojo/                       Article / Category / User / PageBean / Result + 篮球 4 POJO
│       ├── interceptors/LoginInterceptor.java
│       ├── exception/GlobalExceptionHandler.java
│       └── utils/                      JwtUtil / PasswordEncoder / ThreadLocalUtil
│   └── src/main/resources/
│       ├── application.yml
│       └── db/                         schema.sql / schema-basketball.sql
└── big-event-web/                      前端（Vue 3 + Vite）
    ├── index.html                      `<title>Hoops · 篮球数据平台</title>`
    ├── package.json                    name: hoops-web
    ├── package-lock.json               name: hoops-web
    ├── README.md                       Hoops 篮球数据平台前端
    └── src/
        ├── api/                        article.js / category.js / user.js / season.js / team.js / player.js / game.js
        ├── router/index.js           新增 /team /player /season /game，默认重定向 /team
        ├── stores/user.js            + 已删除 counter.js
        ├── utils/request.js
        └── views/                      Login / Layout / ArticleManage / ArticleCategory / UserInfo / UserAvatar / UserPassword
                                        + Season.vue / Team.vue / Player.vue / Game.vue
```

---

## 3. 技术栈、运行环境和启动方式

### 3.1 技术栈

**后端**：Spring Boot 4.0.7 / MyBatis / PageHelper 1.4.7 / MySQL 8.0（库名 `hoops`）/ Redis / JWT / BCrypt / Jakarta Bean Validation 3.0

**前端**：Vue 3 + Vite 8 + Element Plus 2.14.3 + Pinia + Vue Router + Axios + Sass

### 3.2 本机环境

| 工具 | 路径/状态 |
|---|---|
| JDK 17 | `C:\Users\朱毅城\.jdks\ms-17.0.19` |
| Maven 3.9.9 | IDEA 自带 |
| MySQL 8.0 | 服务 `MySQL80`，3306 |
| Redis 5.0.14.1 | 已运行，6379 |
| Node.js | v24.15.0 |

### 3.3 启动方式

后端：

```powershell
cd C:\Users\朱毅城\Desktop\big-event-fullstack\big-event
& 'D:\BaiduNetdiskDownload\IntelliJ IDEA 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' spring-boot:run
```

前端：

```powershell
cd C:\Users\朱毅城\Desktop\big-event-fullstack\big-event-web
npm run dev      # http://localhost:5173
```

### 3.4 当前服务状态

| 服务 | 状态 |
|---|---|
| MySQL (3306) | ✅ 运行中 |
| Redis (6379) | ✅ 运行中 |
| 后端 (8080) | ✅ 运行中（由本会话启动） |
| 前端 (5173) | ✅ 运行中（由本会话启动） |

### 3.5 测试账号

| 用户名 | 密码 |
|---|---|
| `paulgeorge8` | `asdfgh` |

---

## 4. 已经完成的功能

### 4.1 认证与会话

- 注册、登录、JWT + Redis 双重校验
- BCrypt 加盐哈希 + 历史 MD5 兼容自动升级
- JWT 密钥与有效期来自配置，启动校验 ≥256 bit

### 4.2 资讯模块

- 分类 / 文章 CRUD、分页、按分类 + 状态筛选
- `create_user` 归属校验修复横向越权
- 删除分类时外键冲突提示

### 4.3 通用能力

- 图片上传 `/upload` + `/uploads/**` 静态映射
- 全局异常统一处理
- `PageBean` / `Result` 统一响应

### 4.4 数据库

- `schema.sql`：user / category / article
- `schema-basketball.sql`：team / player / season / game / player_game_stat / shot

### 4.5 篮球业务 CRUD（后端）

| 实体 | 接口 |
|---|---|
| Season | POST /season, GET /season, GET /season/{id}, PUT /season, DELETE /season/{id} |
| Team | POST /team, GET /team, GET /team/{id}, PUT /team, DELETE /team/{id} |
| Player | POST /player, GET /player, GET /player/{id}, PUT /player, DELETE /player/{id} |
| Game | POST /game, GET /game, GET /game/{id}, PUT /game, DELETE /game/{id} |

分页参数均有默认值与边界校验（`pageNum>=1`, `1<=pageSize<=100`）。

### 4.6 篮球业务 CRUD（前端）—— 本次新增

| 页面 | 路径 | 功能 |
|---|---|---|
| Season.vue | `/season` | 赛季 CRUD、赛季名模糊搜索、当前赛季标记 |
| Team.vue | `/team` | 球队 CRUD、关键字搜索（name/city）、队标上传、东西部分区 |
| Player.vue | `/player` | 球员 CRUD、按姓名/球队筛选、头像上传、位置选择 |
| Game.vue | `/game` | 比赛 CRUD、按赛季/球队/状态筛选、主客队字典映射、主客不同校验 |

页面模式：复用 `ArticleManage.vue` 的 card + search-form + table + pagination + drawer 骨架；表单校验复用 `ArticleCategory.vue` 的 rules + `formRef.validate()`。

### 4.7 菜单与品牌

- 侧边栏菜单：球队 / 球员 / 赛季 / 比赛 / 资讯 / 个人中心
- Element Plus 接入中文语言包
- 清理旧标识：index.html title、package.json/package-lock.json name、README.md、Layout 页脚
- 删除 `src/stores/counter.js` 脚手架残留

---

## 5. 最近提交

| 提交 | 内容 |
|---|---|
| `df1cc88` | `feat(stats): 进阶数据计算引擎与参数化单元测试` |
| `e95705b` | `feat(tool): shot 造数 Runner，生成 10 万条投篮数据` |
| `053bb6f` | `data(seed): 篮球种子数据与 CSV 导入修复` |
| `360bda2` | `feat(import): CSV 批量导入框架与六实体导入接口` |
| `d6f257d` | `feat(stat,shot): 补齐单场数据与投篮事件实体 CRUD` |
| `0daa638` | `chore(web): 清理旧标识，前端更名 hoops-web` |
| `1734008` | `refactor(web): 菜单重构为篮球域并接入中文语言包` |
| `3de8bc6` | `feat(web): 新增赛季/球队/球员/比赛管理页面` |
| `2d5a376` | `feat(web): 新增赛季/球队/球员/比赛接口模块` |
| `677ea4f` | `docs: 同步人工测试清单 W1 状态` |

`feature/basketball` **尚未推送**到远端。最近一次 `git push` 因无上游分支失败，需要执行 `git push --set-upstream origin feature/basketball`。

---

## 6. 当前正在实现的功能 / 下一步

### W1 已全部完成

- ✅ 洗痕迹（包名 / JWT / BCrypt / README / 库名）
- ✅ 6 张篮球表建模
- ✅ 4 个篮球实体后端 CRUD
- ✅ 4 个篮球实体前端 CRUD
- ✅ 前端菜单重构与品牌清理

### 下一步：W2（已完成，2026-10-02）

按 `DESIGN.md` 进入 W2：

1. **CSV 批量导入**（球队 / 球员 / 赛季 / 比赛 / player_game_stat / shot）
   - 上传 CSV → 逐行校验 → 事务回滚 → 导入报告
   - 返回成功行数、失败行数、失败原因
2. **种子数据整理**：30 队 / 约 300 球员 / 100 场比赛
3. **进阶数据计算引擎**：8 项指标纯函数类 + JUnit 5 参数化测试
4. **排行榜 + Redis 缓存**：窗口函数排名、缓存 key 设计、TTL、性能对比数字
5. **投篮热图后端**：按 8 区域聚合命中率

**已完成**：
- ✅ CSV 批量导入 6 个实体（team / player / season / game / player-game-stat / shot）
- ✅ 种子数据：30 队 / 1 赛季 / 39 核心球员 / 8 场比赛 / 24 条球员单场数据
- ✅ 10 万条 shot 造数 Runner（CommandLineRunner，约 7 秒完成）
- ✅ StatCalculator 8 项进阶指标 + 23 个 JUnit 5 参数化测试（mvn test 全绿）

### 下一步：W3

1. **排行榜接口**（窗口函数排名）
2. **Redis 缓存 + 失效策略**
3. **性能对比测量**：拿到加缓存前/后的 P95 数字
4. **热图聚合接口**：按区域返回命中率
5. **前端新增排行榜、热图可视化页面**（可放到 W4）

### 建议的实施顺序

1. 先整理 CSV 种子数据（最耗时，决定后续所有演示）
2. 实现 CSV 导入接口（一个实体一个实体接入，每完成一个提交）
3. 进阶数据计算引擎 + 单元测试
4. 排行榜接口 + Redis 缓存 + 性能对比
5. 投篮热图聚合接口
6. 前端新增排行榜、热图可视化页面（W3/W4）

---

## 7. 当前遇到的问题 / 风险

### 7.1 已确认但未修复的缺陷

| 编号 | 缺陷 | 状态 |
|---|---|---|
| B2 | 401 后无法跳回登录页（路由守卫与 token 清理冲突） | 未修复 |
| B8 | 用户名可枚举 | 未修复 |
| D16/D17 | 新增/修改文章未校验 `categoryId` 归属 | 未修复 |
| D22/D23 | 切换每页条数不重置页码；删除当前页最后一条不回退 | 未修复 |
| E13/E14 | 改密只失效当次 token | 未修复 |
| E16 | 后端没有 logout 接口 | 未修复 |
| F4/F5/F6/F7 | 上传无白名单、无扩展名抛异常、失败提示成功、XSS 风险 | 未修复 |
| H1/H2/H3 | 分页参数无默认值/边界校验/上限（篮球实体已加，article/category 未加） | 部分修复 |
| Q1 | PageHelper 1.4.7 与 Spring Boot 4 不匹配 | 未修复 |
| Q13/Q14 | 上传返回 URL 硬编码、上传目录漂移 | 未修复 |

### 7.2 环境/约束

- **不可从 Agent shell 执行 `git push`**：沙箱禁止访问 Windows TLS 凭证存储，连百度都访问不了。推送必须由用户在 IDEA 或普通 shell 中执行。
- **Redis 未注册服务**：本会话发现 6379 已有一个 Redis 实例运行；当前实例是端口冲突后发现的既有实例，不是本会话启动的。
- **后端和前端 dev server 由本会话启动**：仍在后台运行。如需释放资源，手动关闭对应命令窗口或进程。

---

## 8. 验证方式

前端页面验证：

```
1. 浏览器打开 http://localhost:5173/login
2. 登录 paulgeorge8 / asdfgh
3. 检查菜单：球队 / 球员 / 赛季 / 比赛 / 资讯 / 个人中心
4. 每个页面测试：列表加载、搜索、重置、翻页、新增、编辑回显、删除
5. Player/Game 页面确认球队/赛季名称显示正确（非数字 ID）
6. 上传功能测试（队标、头像）
7. 回归测试：资讯分类 / 资讯文章 / 个人中心 / 退出登录
```

构建验证：

```powershell
cd C:\Users\朱毅城\Desktop\big-event-fullstack\big-event-web
npm run build
```

后端接口验证（示例）：

```powershell
$TOKEN = (curl -s -X POST http://localhost:8080/user/login -d "username=paulgeorge8&password=asdfgh" | ConvertFrom-Json).data
curl -s -H "Authorization:$TOKEN" "http://localhost:8080/team?pageNum=1&pageSize=5"
```

---

## 9. 给下一个 Agent 的接手指令

你好。你接手时 W1 已全部完成，W2 尚未开始。请按以下要求继续：

### 立即要做的事

1. **读三个文件**：本文件 → `DESIGN.md` → `TEST-CHECKLIST.md` 附录 D
2. **确认环境**：Redis / MySQL / 后端 8080 / 前端 5173 是否仍在运行（本会话启动的进程可能仍在）
3. **从 W2 开始**：CSV 批量导入 → 种子数据 → 计算引擎 → 排行榜缓存 → 投篮热图

### 必须遵守的约束

- **不要修改或新增任何未被要求的功能**。用户明确说过不要提新需求。
- **不要执行 `git push`**。环境限制，交给用户。
- **不要直接运行 `schema.sql` 或 `schema-basketball.sql`**（有 `DROP TABLE`）。
- **不要动用户正在运行的实例**。需要验证时用独立端口（如 8090）起临时后端。
- **写文件一律 UTF-8 无 BOM**，读文件显式指定 UTF-8。
- **不要凭推测写结论**。不确定时问用户。

### 建议的工作节奏

```
读文档 → 确认环境 → 挑一个小块（如 Season CSV 导入）
  → 实现 → mvn compile 验证 → 起临时实例打接口
  → 核对数据库 → 确认原有功能未回归 → 提交 → 下一块
```

---

*文档由本会话结束时生成，反映 2026-10-01 的项目状态。*
