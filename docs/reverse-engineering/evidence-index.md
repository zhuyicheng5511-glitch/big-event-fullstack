# 证据索引（Evidence Index）

> 项目：`big-event-fullstack`（Hoops 篮球数据平台）
> 生成：2026-10-31 · 范围：静态只读分析（未运行应用、未连接数据库/Redis）
> 配套图：`01-architecture.mmd`、`02-module-dependencies.mmd`

## 证据类型约定

| 类型 | 含义 |
|---|---|
| **直接观察** | 本次实际读取了源码/配置原文，可给出文件与行号 |
| **静态推断** | 基于直接观察得出的判断，未逐行核实或未运行时验证，可能被推翻 |
| **未知** | 无法在当前只读条件下验证 |

> 注意：本文件**不披露**任何密钥、口令、token 的真实值。

---

## A. 构建与依赖

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| A1 | Spring Boot parent 版本 **4.0.7** | `big-event/pom.xml:6-8` | 直接观察 | 单 Maven 模块，无多模块聚合 |
| A2 | Java **17** | `big-event/pom.xml:30` | 直接观察 | — |
| A3 | `mybatis-spring-boot-starter` **4.0.1** | `pom.xml:57-58` | 直接观察 | — |
| A4 | `pagehelper-spring-boot-starter` **4.1.1** | `pom.xml:91-92` | 直接观察 | 与 Boot 4 的匹配性未运行时验证 |
| A5 | `spring-boot-starter-data-redis` | `pom.xml:96-97` | 直接观察 | 使用 `StringRedisTemplate` |
| A6 | `java-jwt` **4.4.0** | `pom.xml:36-37` | 直接观察 | — |
| A7 | `commons-csv` **1.12.0** | `pom.xml:107` | 直接观察 | CSV 导入用 |
| A8 | `springdoc-openapi-starter-webmvc-ui` **2.7.0** | `pom.xml:113` | 直接观察 | 文档路径见 `application.yml:41-47` |
| A9 | `mysql-connector-j` | `pom.xml:63` | 直接观察 | — |

---

## B. 入口、鉴权与会话

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| B1 | 拦截器在 `WebConfig` 注册，排除 `/user/login`、`/user/register`、`/uploads/**`、`/error`、`/swagger-ui/**`、`/v3/api-docs/**`、`/swagger-ui.html` | `config/WebConfig.java:17-22` | 直接观察 | 其余路径全部受拦截 |
| B2 | `preHandle`：取 `Authorization` 头 → `Redis get(token)`；为 `null` 直接返回 **401** | `interceptors/LoginInterceptor.java:25-43` | 直接观察 | 鉴权同时依赖 Redis 与 JWT |
| B3 | Redis 命中后 `jwtUtil.parseToken(token)` 并把 claims 放入 `ThreadLocalUtil` | `LoginInterceptor.java:37-38` | 直接观察 | — |
| B4 | `afterCompletion` 清理 `ThreadLocal` | `LoginInterceptor.java:46-50` | 直接观察 | 注释说明为防内存泄漏 |
| B5 | 登录成功写 Redis：**key = token 本身**，value = token，TTL = `jwt.expire-hours`（小时） | `controller/UserController.java:79-81` | 直接观察 | key 无命名空间前缀 |
| B6 | 改密成功后删除**当前** token | `UserController.java:128-129` | 直接观察 | 其他设备的 token 是否失效，本文件未验证 |
| B7 | JWT 密钥与有效期全部来自配置（`jwt.secret` / `jwt.expire-hours`），启动时校验密钥长度与有效期为正 | `utils/JwtUtil.java:15, 28-44` | 直接观察 | 具体密钥值不在此披露 |
| B8 | `expire-hours: 12`；`secret` 有开发默认值，可由环境变量覆盖 | `application.yml:37-39` | 直接观察 | 生产应覆盖，不在本文件展开值 |
| B9 | 登录含历史密码迁移：`needsUpgrade` 命中则改用 BCrypt 重存 | `UserController.java:68-72` | 直接观察 | — |

---

## C. 数据访问与事务

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| C1 | 共 **9 张表**：`user` / `category` / `article`（`schema.sql`）+ `team` / `player` / `season` / `game` / `player_game_stat` / `shot`（`schema-basketball.sql`） | `schema.sql:39,58,76`；`schema-basketball.sql:35,55,82,100,128,172` | 直接观察 | 字段/索引/外键未逐行阅读（见 H4） |
| C2 | MyBatis `map-underscore-to-camel-case: true` | `application.yml:20-22` | 直接观察 | — |
| C3 | `resources/` 下**无 Mapper XML**，Mapper 全为接口 + 注解 SQL | `resources` 目录清单（仅 `application.yml`、`db/*.sql`、`seed/*.csv`） | 直接观察 | 已见 `@Select/@Insert/@Update/@Delete` |
| C4 | 全项目**唯一**显式事务：`ImportServiceImpl` 上的 `@Transactional(rollbackFor = Exception.class)` | `service/impl/ImportServiceImpl.java:46`；`git grep @Transactional` 仅此 1 处 | 直接观察 | 其他业务方法无声明式事务 |
| C5 | `ImportServiceImpl` 直接依赖 6 个 Mapper（Team/Player/Season/Game/PlayerGameStat/Shot） | `ImportServiceImpl.java:3, 30-40` | 直接观察 | 按实体分别写入 |
| C6 | `POST /import/{entity}`：以 `MultipartFile` 入参；失败行数 > 0 时返回 `code=1` | `controller/ImportController.java:11, 17-25` | 直接观察 | — |
| C7 | `PageHelper.startPage` 出现在 **7 个** ServiceImpl（Article/Game/PlayerGameStat/Player/Season/Shot/Team） | 各文件第 28/32/35 行附近 | 直接观察 | 其余 Impl 未用分页 |
| C8 | 13 个 Controller 的路径前缀：`/user`、`/article`、`/category`、`/team`、`/player`、`/season`、`/game`、`/player-game-stat`、`/rank`、`/shot`、`/import`，以及无前缀的 `POST /upload` | 12 个 `@RequestMapping` + `FileUploadController.java:21` | 直接观察 | — |
| C9 | `ShotController` 与 `ShotChartController` **共用 `/shot` 前缀** | `ShotController.java:15`、`ShotChartController.java:17` | 直接观察 | 路由细节见 H2 |
| C10 | 排行榜接口参数：`seasonId` 必填（`@NotNull`）、`teamId`/`position` 可选、`limit` 默认 10 且 1..100 | `controller/RankController.java:26-37` | 直接观察 | 用 `required=false` 让 `@NotNull` 提示生效 |

---

## D. Redis 使用

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| D1 | 排行榜缓存前缀 `rank:points:` | `service/impl/RankServiceImpl.java:29` | 直接观察 | — |
| D2 | TTL：正常结果 **10 分钟**；空结果 **1 分钟** | `RankServiceImpl.java:30-31, 56` | 直接观察 | 空结果短 TTL 属防穿透设计 |
| D3 | 缓存 key 结构：`rank:points:{seasonId}:{teamId|all}:{position|all}:{limit}` | `RankServiceImpl.java:85-90` | 直接观察 | — |
| D4 | 读路径：先 `get`；命中且非空 → Jackson 反序列化直接返回 | `RankServiceImpl.java:37-45` | 直接观察 | — |
| D5 | 反序列化异常 → 删除该 key 并回源数据库 | `RankServiceImpl.java:44-49` | 直接观察 | — |
| D6 | 缓存写入失败被 `catch` 吞掉（仅影响性能，不影响结果） | `RankServiceImpl.java:54-60` | 直接观察 | — |
| D7 | 缓存失效用 `SCAN`（`count(200)`）+ 攒批 `delete`，而非 `KEYS` | `RankServiceImpl.java:66-83` | 直接观察 | — |
| D8 | 会话与排行缓存共用同一个 `StringRedisTemplate`，但 key 空间无统一前缀规范（会话 key 就是 token） | `UserController.java:80-81`、`RankServiceImpl.java:29` | 直接观察 | 仅陈述事实，不作评价 |

---

## E. 前端接入

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| E1 | axios `baseURL='/api'`，`timeout: 5000` | `big-event-web/src/utils/request.js:8-9` | 直接观察 | — |
| E2 | Vite 代理：`/api` → `http://localhost:8080`，`rewrite` 剥掉 `/api`；`/uploads` 原样转发 | `big-event-web/vite.config.js:15-24` | 直接观察 | — |
| E3 | 401 处理：清 token + 跳 `/login` | `request.js:39-45` | 直接观察 | — |
| E4 | 前端规模：`api/` 10 个模块、`views/` 14 个 `.vue`、`components/` 3 个 | `big-event-web/src` 目录清单 | 直接观察 | — |

---

## F. 部署与启动期任务

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| F1 | nginx：`/api/` → `backend:8080/`（剥前缀）；`/uploads/`、`/swagger-ui`、`/v3/api-docs` 透传；`client_max_body_size 10m` | `big-event-web/nginx.conf` | 直接观察 | — |
| F2 | docker-compose 共 **5 个服务**：`mysql`、`redis`、`backend`、`seed`、`frontend` | `docker-compose.yml:2, 20, 31, 57, 74` | 直接观察 | `seed` 为一次性任务 |
| F3 | `/uploads/**` 静态映射到 `System.getProperty("user.dir") + "/uploads/"` | `config/WebConfig.java:25-32` | 直接观察 | 目录随启动位置漂移 |
| F4 | multipart 单文件/单请求上限均为 **10MB** | `application.yml:9-12` | 直接观察 | 与 nginx `client_max_body_size` 对齐 |
| F5 | 4 个 `CommandLineRunner` 均为**参数触发**：`--import-seed`、`--generate-stats`、`--generate-shots`、`--perf-test` | 各 Runner 的 `shouldRun` | 直接观察 | 不带参数启动时不会执行 |
| F6 | `SeedDataRunner` 依赖 `ImportService` + `Season/Team/Player` Mapper；幂等判断读三张表是否非空 | `runner/SeedDataRunner.java:3-9, 42-55, 111-113` | 直接观察 | — |
| F7 | 数据生成器与性能测试器依赖 Mapper / RankService | `PlayerGameStatDataGeneratorRunner.java:25-34`、`ShotDataGeneratorRunner.java:16-21`、`RankPerfTestRunner.java:17-19` | 直接观察 | — |

---

## G. 测试

| 编号 | 结论 | 证据 | 类型 | 注意事项 |
|---|---|---|---|---|
| G1 | `src/test` 仅 2 个测试类 | 目录清单 | 直接观察 | — |
| G2 | `HoopsApplicationTests.contextLoads` 被 `@Disabled("需要本地 MySQL/Redis 服务，CI 环境跳过")` | `src/test/.../HoopsApplicationTests.java:10-12` | 直接观察 | 需本地环境，CI 跳过 |
| G3 | `StatCalculatorTest` 覆盖 FG% / 3P% / eFG% / TS% / EFF / per36 / 两双判定，含除零与参数化用例 | `src/test/.../StatCalculatorTest.java` | 直接观察 | — |

---

## H. 未验证 / 不确定项

| 编号 | 事项 | 说明 | 类型 |
|---|---|---|---|
| H1 | **`StatCalculator` 在 `src/main` 中无任何引用** | `git grep StatCalculator -- big-event/src/main` 仅命中类自身定义与构造器；调用只出现在测试里。说明生产代码当前未使用该计算器。**这是事实陈述，不是缺陷判定** | 直接观察 |
| H2 | `/shot/chart` 与 `/shot/{id}` 的路由优先级 | `ShotChartController:24` 为 `/chart`，`ShotController:42` 为 `/{id}`；按 Spring 字面量优先规则应可共存，但**本次未运行验证** | 静态推断（未验证） |
| H3 | `ImportServiceImpl` 的 CSV 解析、校验、BOM/行号处理细节 | 只确认了入口、事务注解与依赖的 Mapper；未逐行阅读实现 | 未知 |
| H4 | `game` / `shot` 等表的字段、索引、外键约束 | 只读取了 `CREATE TABLE` 行号，未读全文 | 未知 |
| H5 | 4 个 Runner 的实际执行效果与顺序 | 技能规定未经批准不运行代码；只静态确认触发参数 | 未知 |
| H6 | 「其余 10 个 Controller → 对应 Service」「其余 9 个 ServiceImpl → Mapper」的逐条对应 | 图中以 `OC → OS`、`OS → OM` 概括，未逐个核实 `@Autowired` | 静态推断 |
| H7 | 数据量（如 `shot` 行数） | 属运行期观察，非本次静态分析结论，故不写入架构图 | 未知 |
| H8 | Redis 会话 key 无命名空间是否造成运维问题 | 属设计评价，超出本次「逆向理解」范围，未展开 | 未知 |

---

## 如何复核本文件

在 Windows PowerShell 中（只读，不需要启动应用）：

```powershell
# 依赖版本
Select-String -Path big-event/pom.xml -Pattern 'spring-boot-starter-parent|<java.version>|mybatis|pagehelper|java-jwt|springdoc|commons-csv'

# 唯一的 @Transactional
git grep -n '@Transactional' -- big-event/src/main

# Redis key / TTL
git grep -n 'CACHE_PREFIX|CACHE_TTL|opsForValue|expire' -- big-event/src/main

# StatCalculator 是否被生产代码引用
git grep -n 'StatCalculator' -- big-event/src/main
```

若上述命令结果与表格不一致，以源码为准，并更新本文件。
