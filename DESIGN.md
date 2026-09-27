# 篮球数据统计平台 · 设计方案

> **定位**：把原「大事件」通用内容管理项目，重构为**篮球数据统计平台**。
> **目标岗位**：全栈工程师（暑期实习）
> **周期**：1 个月（约 80~100 小时）
> **起点**：`baseline-before-refactor` tag（主流程可运行，已修复越权与接口缺陷）
> **本文档的作用**：整个月的施工图 + 验收标准。开发中随时回头对照，避免跑偏。

---

## 一、项目定位与目标

### 要解决的问题

篮球迷（含我们自己）分散在虎扑、NBA 官网、各种数据站看数据，缺少一个**可自建、可查询、可视化**的数据平台。本项目提供：

- 球队 / 球员 / 赛季 / 比赛的结构化数据管理
- 单场技术统计的录入与批量导入
- **进阶数据自动计算**（不只是得分篮板助攻）
- **排行榜**与**投篮热图**可视化

### 目标（必须达成）

| # | 目标 | 验收标准 |
|---|---|---|
| G1 | 6 张业务表的完整 CRUD + 分页筛选 | 接口可用，前端可操作 |
| G2 | CSV 批量导入比赛数据 | 支持 1000 行导入，含校验与错误回滚 |
| G3 | 进阶数据计算引擎 | 8 项指标，单元测试覆盖，与公开数据源核对一致 |
| G4 | 排行榜 + Redis 缓存 | 有性能对比数字（加缓存前后） |
| G5 | 投篮热图 | 后端按区域聚合 + 前端 ECharts 半场图 |
| G6 | 工程化 | Docker Compose 一键起、CI 通过、接口文档可访问 |

### 非目标（明确不做，防止 scope 蔓延）

- ❌ **PER（球员效率值）** —— 需要联盟平均、节奏系数等上下文，公式争议大，一个月做不完且容易算错
- ❌ 实时比赛直播数据接入
- ❌ 移动端 App / 小程序
- ❌ 视频、社交、评论、点赞
- ❌ 微服务拆分（单体足够，别为了"显得高级"而拆）
- ❌ 全量 TypeScript 迁移（新模块用 TS，老代码不动）

> **scope 纪律**：非目标清单和目标清单一样重要。每次想加功能前先问：它服务于 G1~G6 中的哪一条？答不上来就不做。

---

## 二、领域模型

### 2.1 数据归属划分（重要）

| 数据类型 | 表 | 归属 | 权限 |
|---|---|---|---|
| **业务数据** | `team` `player` `season` `game` `player_game_stat` `shot` | **全局共享** | 管理员可写，所有人可读 |
| **用户数据** | `article` `category` | **按创建人隔离** | 沿用现有 `create_user` 校验 |

这个划分直接对应后续要加的 RBAC：**管理员维护数据，普通用户只能看**。注意业务数据**不要**加 `create_user`，否则公共数据会被切碎，排行榜就算不出全联盟的了。

### 2.2 表结构

```sql
-- 球队
CREATE TABLE `team` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name`        varchar(50)  NOT NULL                COMMENT '球队名，如 洛杉矶湖人',
  `name_en`     varchar(80)  DEFAULT ''              COMMENT '英文名，如 Lakers',
  `city`        varchar(50)  DEFAULT ''              COMMENT '城市',
  `conference`  varchar(10)  DEFAULT ''              COMMENT '东部/西部',
  `division`    varchar(20)  DEFAULT ''              COMMENT '分区，如 太平洋区',
  `logo_url`    varchar(255) DEFAULT ''              COMMENT '队标地址',
  `create_time` datetime     NOT NULL,
  `update_time` datetime     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队表';

-- 球员
CREATE TABLE `player` (
  `id`            int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name`          varchar(50)  NOT NULL                COMMENT '中文名',
  `name_en`       varchar(80)  DEFAULT ''              COMMENT '英文名',
  `team_id`       int unsigned DEFAULT NULL            COMMENT '所属球队ID',
  `jersey_number` varchar(5)   DEFAULT ''              COMMENT '球衣号',
  `position`      varchar(10)  DEFAULT ''              COMMENT '位置 PG/SG/SF/PF/C',
  `height_cm`     smallint unsigned DEFAULT NULL       COMMENT '身高cm',
  `weight_kg`     smallint unsigned DEFAULT NULL       COMMENT '体重kg',
  `birth_date`    date         DEFAULT NULL            COMMENT '出生日期',
  `country`       varchar(30)  DEFAULT ''              COMMENT '国籍',
  `avatar_url`    varchar(255) DEFAULT ''              COMMENT '头像地址',
  `is_active`     tinyint(1)   NOT NULL DEFAULT 1      COMMENT '是否现役',
  `create_time`   datetime     NOT NULL,
  `update_time`   datetime     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_player_team` (`team_id`),
  CONSTRAINT `fk_player_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员表';

-- 赛季
CREATE TABLE `season` (
  `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name`        varchar(20)  NOT NULL                COMMENT '赛季名，如 2025-26',
  `start_date`  date         DEFAULT NULL            COMMENT '开始日期',
  `end_date`    date         DEFAULT NULL            COMMENT '结束日期',
  `is_current`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否当前赛季',
  `create_time` datetime     NOT NULL,
  `update_time` datetime     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_season_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛季表';

-- 比赛
CREATE TABLE `game` (
  `id`            int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `season_id`     int unsigned NOT NULL                COMMENT '赛季ID',
  `home_team_id`  int unsigned NOT NULL                COMMENT '主队ID',
  `away_team_id`  int unsigned NOT NULL                COMMENT '客队ID',
  `game_date`     datetime     NOT NULL                COMMENT '比赛时间',
  `home_score`    smallint unsigned DEFAULT NULL       COMMENT '主队得分',
  `away_score`    smallint unsigned DEFAULT NULL       COMMENT '客队得分',
  `status`        varchar(10)  NOT NULL DEFAULT '已结束' COMMENT '未开始/进行中/已结束',
  `venue`         varchar(80)  DEFAULT ''              COMMENT '场馆',
  `create_time`   datetime     NOT NULL,
  `update_time`   datetime     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_game_season_date` (`season_id`, `game_date`),
  KEY `idx_game_home` (`home_team_id`),
  KEY `idx_game_away` (`away_team_id`),
  CONSTRAINT `fk_game_season` FOREIGN KEY (`season_id`)     REFERENCES `season` (`id`),
  CONSTRAINT `fk_game_home`   FOREIGN KEY (`home_team_id`)  REFERENCES `team` (`id`),
  CONSTRAINT `fk_game_away`   FOREIGN KEY (`away_team_id`)  REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛表';

-- 球员单场数据（核心事实表，数据量最大，性能优化主要针对它）
CREATE TABLE `player_game_stat` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `game_id`     int unsigned NOT NULL                COMMENT '比赛ID',
  `player_id`   int unsigned NOT NULL                COMMENT '球员ID',
  `team_id`     int unsigned NOT NULL                COMMENT '所属球队ID',
  `is_starter`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否首发',
  `minutes`     decimal(4,1) NOT NULL DEFAULT 0.0    COMMENT '上场分钟',
  `points`      smallint unsigned NOT NULL DEFAULT 0 COMMENT '得分',
  `rebounds`    smallint unsigned NOT NULL DEFAULT 0 COMMENT '篮板',
  `assists`     smallint unsigned NOT NULL DEFAULT 0 COMMENT '助攻',
  `steals`      smallint unsigned NOT NULL DEFAULT 0 COMMENT '抢断',
  `blocks`      smallint unsigned NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnovers`   smallint unsigned NOT NULL DEFAULT 0 COMMENT '失误',
  `fouls`       smallint unsigned NOT NULL DEFAULT 0 COMMENT '犯规',
  `fgm`         smallint unsigned NOT NULL DEFAULT 0 COMMENT '投篮命中',
  `fga`         smallint unsigned NOT NULL DEFAULT 0 COMMENT '投篮出手',
  `fg3m`        smallint unsigned NOT NULL DEFAULT 0 COMMENT '三分命中',
  `fg3a`        smallint unsigned NOT NULL DEFAULT 0 COMMENT '三分出手',
  `ftm`         smallint unsigned NOT NULL DEFAULT 0 COMMENT '罚球命中',
  `fta`         smallint unsigned NOT NULL DEFAULT 0 COMMENT '罚球出手',
  `plus_minus`  smallint NOT NULL DEFAULT 0          COMMENT '正负值',
  `create_time` datetime     NOT NULL,
  `update_time` datetime     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_game_player` (`game_id`, `player_id`),
  KEY `idx_pgs_player` (`player_id`),
  CONSTRAINT `fk_pgs_game`   FOREIGN KEY (`game_id`)   REFERENCES `game` (`id`),
  CONSTRAINT `fk_pgs_player` FOREIGN KEY (`player_id`) REFERENCES `player` (`id`),
  CONSTRAINT `fk_pgs_team`   FOREIGN KEY (`team_id`)   REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员单场数据表';

-- 投篮事件（热图数据基础，数据量最大，专门用于性能演示）
CREATE TABLE `shot` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `game_id`     int unsigned NOT NULL                COMMENT '比赛ID',
  `player_id`   int unsigned NOT NULL                COMMENT '球员ID',
  `quarter`     tinyint unsigned NOT NULL            COMMENT '节次 1-4，加时 5+',
  `game_clock`  varchar(10)  DEFAULT ''              COMMENT '本节剩余时间 mm:ss',
  `x`           decimal(5,2) NOT NULL                COMMENT '横坐标(英尺) 0-50',
  `y`           decimal(5,2) NOT NULL                COMMENT '纵坐标(英尺) 0-47，0为底线',
  `distance_ft` decimal(5,2) DEFAULT NULL            COMMENT '出手距离(英尺)',
  `shot_type`   varchar(10)  NOT NULL                COMMENT '两分/三分/罚球',
  `made`        tinyint(1)   NOT NULL                COMMENT '是否命中',
  `zone`        varchar(20)  NOT NULL                COMMENT '区域，见下方枚举',
  `assisted_by` int unsigned DEFAULT NULL            COMMENT '助攻人球员ID',
  `create_time` datetime     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shot_game_player` (`game_id`, `player_id`),
  KEY `idx_shot_zone` (`zone`),
  CONSTRAINT `fk_shot_game`   FOREIGN KEY (`game_id`)   REFERENCES `game` (`id`),
  CONSTRAINT `fk_shot_player` FOREIGN KEY (`player_id`) REFERENCES `player` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投篮事件表';
```

> **`shot.zone` 枚举**（区域划分是热图的核心逻辑）：
> `篮下` / `油漆区` / `中距离` / `左底角三分` / `右底角三分` / `左翼三分` / `右翼三分` / `弧顶三分`

### 2.3 与现有表的关系

| 现有表 | 重构后 | 改动 |
|---|---|---|
| `user` | 用户（后续加 `role` 字段） | 加列 |
| `category` | **资讯分类**（战报/交易/伤病/数据） | 不改 |
| `article` | **赛事资讯 / 比赛战报** | 不改 |
| — | 上面 6 张新表 | 新增 |

现有 `article` / `category` / 分页 / 筛选 / 上传 / 全局异常 / 参数校验 **全部保留复用**。

---

## 三、数据方案

### 3.1 CSV 主链路（最高优先级）

**为什么**：可复现、可控、演示零风险，且"批量导入"本身就是一个像样的功能。

**CSV 格式设计**（每个实体一个文件，放在 `big-event/src/main/resources/seed/`）：

```csv
# players.csv
name,name_en,team,position,jersey_number,height_cm,weight_kg,birth_date,country
勒布朗·詹姆斯,LeBron James,洛杉矶湖人,SF,23,206,113,1984-12-30,美国
```

```csv
# player_game_stats.csv
game_key,player,team,is_starter,minutes,points,rebounds,assists,steals,blocks,turnovers,fouls,fgm,fga,fg3m,fg3a,ftm,fta,plus_minus
2025-10-22_LAL_GSW,勒布朗·詹姆斯,洛杉矶湖人,1,35.5,28,8,11,2,1,3,2,10,18,3,7,5,6,12
```

```csv
# shots.csv
game_key,player,quarter,game_clock,x,y,distance_ft,shot_type,made,zone
2025-10-22_LAL_GSW,勒布朗·詹姆斯,1,10:32,25.0,4.0,2.0,两分,1,篮下
```

**导入功能要求**（这就是功能本身的验收标准）：

1. 上传 CSV → 逐行校验（字段缺失、格式错误、外键不存在、重复数据）
2. **事务保证**：任一行失败 → 整体回滚，不允许"导入一半"
3. 返回**导入报告**：成功 N 行、失败 M 行、每条失败的行号与原因
4. 批量插入（`INSERT ... VALUES (...),(...)` 分批提交），不能逐行 insert
5. 幂等：重复导入同一文件不产生重复数据（靠唯一索引）

### 3.2 数据量策略

| 用途 | 数据量 | 怎么来 |
|---|---|---|
| 功能演示 | 30 支球队 / 约 300 名球员 / 100 场比赛 | 手工整理的真实数据 CSV |
| **性能演示** | **10 万条 `shot` + 3 万条 `player_game_stat`** | 写一个生成脚本（可复用同一套 CSV 导入接口，或直接 SQL 批量生成） |

> 10 万条 `shot` 是**热图聚合和排行榜缓存的性能测试基础**，也是简历里"性能对比数字"的来源。**没有数据量就没有性能优化可言**，这一步不能省。

### 3.3 公开 API（有时间再做）

留到 W4 有余力时作为增强：定时任务从公开 NBA 数据源同步当日比赛结果，写入 `game` / `player_game_stat`。
技术点：`@Scheduled`、HTTP 客户端、限流与重试、失败告警、断点续传。
**注意**：它只是加分项，不是主链路 —— 答辩现场绝不能依赖它。

---

## 四、三个技术亮点

### ⭐ 亮点 1：投篮热图 Shot Chart（视觉冲击力最强）

**后端**
- `GET /api/shot/chart?playerId=&seasonId=&zone=` 按区域聚合：命中数、出手数、命中率
- 聚合 SQL 示例：
  ```sql
  SELECT zone, COUNT(*) AS attempts, SUM(made) AS made,
         ROUND(SUM(made) / COUNT(*) * 100, 1) AS fg_pct
  FROM shot
  WHERE player_id = ? AND game_id IN (SELECT id FROM game WHERE season_id = ?)
  GROUP BY zone;
  ```
- 返回每个区域的**中心点坐标 + 命中率**，供前端渲染

**前端**
- ECharts 半场图（`graphic` 画球场底线/三分线/油漆区）+ `scatter`/`heatmap` 叠加
- 支持按球员 / 球队 / 赛季切换，鼠标悬停显示"该区域 12 投 7 中，58.3%"

**可讲的技术点**：坐标归一化、区域划分算法、聚合查询优化、Canvas 渲染性能

### ⭐ 亮点 2：进阶数据计算引擎 + 单元测试（体现工程严谨性）

**要实现的指标**（8 项，全在 `StatCalculator` 纯函数类里，无副作用、易测）：

| 指标 | 公式 | 边界 |
|---|---|---|
| 投篮命中率 FG% | `FGM / FGA` | `FGA=0` |
| 三分命中率 3P% | `FG3M / FG3A` | `FG3A=0` |
| 罚球命中率 FT% | `FTM / FTA` | `FTA=0` |
| **有效命中率 eFG%** | `(FGM + 0.5 × FG3M) / FGA` | `FGA=0` |
| **真实命中率 TS%** | `PTS / (2 × (FGA + 0.44 × FTA))` | `FGA=FTA=0` |
| 效率值 EFF | `(PTS+REB+AST+STL+BLK) − (FGA−FGM) − (FTA−FTM) − TOV` | 无 |
| 每36分钟数据 | `数据 / MIN × 36` | `MIN=0` |
| 两双/三双判定 | 统计 ≥10 的分项个数 | 无 |

**测试要求**（这是本亮点的核心价值）：
- 用 JUnit 5 **参数化测试**（`@ParameterizedTest` + `@CsvSource`）覆盖正常值
- 专门覆盖**除零边界**，断言返回 `null` 或 `0` 而不是抛异常或 `NaN`
- 用**真实球员赛季数据**做端到端核对，断言与公开数据源一致（精度容差 0.1%）

> 简历上这句话的分量：**"我实现的 TS% 公式是 `PTS / (2 × (FGA + 0.44 × FTA))`，并用真实球员赛季数据编写参数化单元测试验证，与公开数据源数值一致。"**

### ⭐ 亮点 3：排行榜 + Redis 缓存 + 性能对比（产出量化数字）

**接口**：`GET /api/rank/points?seasonId=&teamId=&position=&limit=`

**实现要点**
- 聚合 SQL：`SUM(points)` 按球员分组，支持按球队/位置过滤
- 需要**窗口函数**做排名：`RANK() OVER (ORDER BY SUM(points) DESC)`
- Redis 缓存：`@Cacheable` 或手动 `StringRedisTemplate`
  - key 设计：`rank:points:{seasonId}:{teamId}:{position}:{limit}`
  - TTL 10 分钟 + 数据变更时主动失效
  - **缓存穿透防护**：空结果也缓存短 TTL（防恶意查询不存在的数据）
- **性能对比**：同一接口在 3 万条 `player_game_stat` 数据下，记录加缓存前/后的 P95

**产出**：一个真实的数字，例如 `320ms → 12ms`。**这个数字是简历里最值钱的东西之一。**

---

## 五、四周排期

### W1 · 洗痕迹 + 建模 + 基础 CRUD

| 任务 | 验收 |
|---|---|
| 洗掉教程痕迹（见下） | `itheima` 消失、包名改掉、README 重写 |
| 6 张表 DDL 落库（Flyway 或 schema.sql） | 表建好、外键正确 |
| 球队/球员/赛季/比赛的 CRUD + 分页筛选 | 接口可用、前端可操作 |
| 前端菜单重构 | 侧边栏换成篮球域菜单 |

**洗痕迹清单**（W1 必做，详见项目根目录的评审结论）：
1. JWT 密钥移到配置项，删掉 `itheima`
2. 包名 `com.example.bigevent` → `com.zhuyicheng.hoops`（或你定的名字）
3. `Md5Util` → `BCryptPasswordEncoder`（**先** `ALTER TABLE user MODIFY password varchar(100)`）
4. README 全部重写 + 修正 4 处与实现不符的描述
5. 删掉仓库里的 `test.jpg` 或移出（如果你想彻底干净）

### W2 · 数据导入 + 计算引擎

| 任务 | 验收 |
|---|---|
| CSV 导入功能（校验 + 事务 + 报告） | 导入 1000 行成功；错误行能回滚并给出原因 |
| 整理种子数据（30 队 / 300 人 / 100 场） | `SELECT COUNT(*)` 数据到位 |
| 造数脚本（10 万条 shot） | 数据量达标，插入耗时可接受 |
| **进阶数据计算引擎 + 单元测试** | 8 项指标，测试全绿，与公开数据核对一致 |

### W3 · 排行榜 + 缓存 + 热图后端

| 任务 | 验收 |
|---|---|
| 排行榜接口（窗口函数） | 排名正确 |
| Redis 缓存 + 失效策略 | 缓存命中率可见 |
| **性能对比测量** | 拿到加缓存前/后的 P95 数字 |
| 热图聚合接口 | 按区域返回命中率 |
| 前端大表格虚拟滚动 | 10 万条不卡 |

### W4 · 前端可视化 + 工程化 + 收尾

| 任务 | 验收 |
|---|---|
| **投篮热图前端**（ECharts） | 半场图 + 区域命中率，可切换维度 |
| 球员雷达图 / 赛季趋势图 | 至少 2 个可视化 |
| 组件抽象（`ShotChart` / `PlayerRadar` / `StatTable`） | 可复用、有 props 定义 |
| Docker Compose 一键启动 | `docker compose up` 起 MySQL+Redis+后端+前端 |
| GitHub Actions CI | build + test 通过 |
| springdoc 接口文档 | `/swagger-ui` 可访问 |
| README + 架构图 + ER 图 + 演示截图 | 简历可直接引用 |
| 简历文案定稿 | 见下节 |

> **节奏纪律**：每完成一块就 **验证 + git 提交**，始终保持"能跑"。不要连续改三天再调试。
> 每完成一个亮点，在 README 里记一行"做了什么 + 量化结果"，月底直接抄进简历。

---

## 六、简历文案（提前写，倒推开发）

**先写简历，再倒推要做什么** —— 这样能保证每一行代码都服务于简历，而不是做完再想怎么吹。

### 目标文案（月底必须全部成真）

> **篮球数据统计平台**（课程项目重构｜全栈｜后端 ~50 接口 / 前端 ~10 页面）
>
> - 将原通用内容管理项目重构为篮球数据统计平台，独立设计 6 张表的数据模型（球队 / 球员 / 赛季 / 比赛 / 单场数据 / 投篮事件），支撑多维度统计查询
> - 实现 **CSV 批量导入**（逐行校验 + 事务回滚 + 导入报告），单次导入 1000 行，任一行失败整体回滚并定位到具体行号与原因
> - 实现**进阶数据计算引擎**（eFG% / TS% / 效率值等 8 项指标），用真实球员赛季数据编写参数化单元测试验证，与公开数据源数值一致
> - 基于 Redis 实现排行榜缓存与失效策略，联盟得分榜接口在 3 万条数据下 **P95 从 xxx ms 降至 xx ms**
> - 实现**投篮热图**：后端按 8 个区域聚合命中率，前端用 ECharts 渲染半场分布图，支持球员 / 球队 / 赛季维度切换
> - 基于 Docker Compose + GitHub Actions 实现一键启动与自动化构建，接入 springdoc 生成接口文档

### 面试要能答上的问题（自测清单）

- [ ] 为什么球队/球员不加 `create_user`，而文章加？（答：公共数据 vs 用户数据的权限模型差异）
- [ ] TS% 公式里那个 `0.44` 是什么？（答：罚球折算成进攻回合的经验系数）
- [ ] 导入为什么必须用事务？批量插入怎么优化？
- [ ] 排行榜为什么用窗口函数而不是应用层排序？
- [ ] 缓存 key 怎么设计？什么时候失效？怎么防穿透？
- [ ] 热图为什么在后端聚合而不是把 10 万条原始坐标丢给前端？
- [ ] 加缓存前的慢在哪？（要有 `EXPLAIN` 的实际分析，不能只背结论）

---

## 七、风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| **种子数据整理耗时超预期** | 阻塞 W2 之后所有工作 | W1 就开始整理；先用少量真实数据跑通流程，再补量 |
| 一个月做不完 3 个亮点 | 每个都半成品 | 优先级：亮点2 → 亮点3 → 亮点1（前两个是简历必需，热图是加分） |
| 造数脚本写不出来 | 没有性能数字 | 退而求其次：用 SQL 存储过程或 `INSERT ... SELECT` 自乘生成 |
| 前端可视化难度超预期 | 热图做不出来 | 先做静态图（固定数据），再做交互；ECharts 官方示例可参考 |
| 中途改崩了 | 项目不可运行 | 随时 `git reset --hard baseline-before-refactor` 回退；每完成一块就提交 |
| scope 蔓延（想加评论/点赞/社交） | 一个月全废 | 严格对照第一节的「非目标」清单 |

---

## 八、下一步

1. 在 `baseline-before-refactor` 基础上建分支：`git checkout -b feature/basketball`
2. 从 W1 第一项「洗掉教程痕迹」开始
3. 每完成一项：验证 → 提交 → 回到本文档打勾

---

*文档创建于项目重构启动前，随开发进展更新。*
