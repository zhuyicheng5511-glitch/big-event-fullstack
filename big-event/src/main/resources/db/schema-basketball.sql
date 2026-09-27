-- ============================================================================
--  篮球数据统计平台（hoops）数据库建表脚本 —— 篮球业务表
-- ============================================================================
--  前置条件：请先执行同目录下的 schema.sql（创建 hoops 库与 user/category/article）
--  环境要求：MySQL 8.0+
--  对应实体：com.zhuyicheng.hoops.pojo.{Team,Player,Season,Game,PlayerGameStat,Shot}
--
--  权限设计说明：
--    这 6 张表存放的是【全局共享的业务数据】（球队、球员、比赛、统计数据），
--    因此【刻意不加 create_user 字段】—— 加了会把公共数据按创建人切碎，
--    导致全联盟排行榜、跨球队对比这类需求无法实现。
--    与之相对，article / category 属于【用户数据】，保留 create_user 做归属隔离。
--    这个划分会在后续引入 RBAC（管理员维护数据 / 普通用户只读）时继续沿用。
--
--  使用方法：
--      mysql -uroot -p < big-event/src/main/resources/db/schema-basketball.sql
--
--  ⚠️ 本脚本含 DROP TABLE，重复执行会清空这 6 张篮球业务表的数据。
-- ============================================================================

USE `hoops`;

-- 删除顺序按外键依赖倒序：被引用的表最后删
DROP TABLE IF EXISTS `shot`;
DROP TABLE IF EXISTS `player_game_stat`;
DROP TABLE IF EXISTS `game`;
DROP TABLE IF EXISTS `player`;
DROP TABLE IF EXISTS `season`;
DROP TABLE IF EXISTS `team`;


-- ----------------------------------------------------------------------------
-- 球队
-- ----------------------------------------------------------------------------
CREATE TABLE `team` (
    `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`        varchar(50)  NOT NULL                COMMENT '球队名，如 洛杉矶湖人',
    `name_en`     varchar(80)  DEFAULT ''              COMMENT '英文名，如 Lakers',
    `city`        varchar(50)  DEFAULT ''              COMMENT '城市',
    `conference`  varchar(10)  DEFAULT ''              COMMENT '东部/西部',
    `division`    varchar(20)  DEFAULT ''              COMMENT '分区，如 太平洋区',
    `logo_url`    varchar(255) DEFAULT ''              COMMENT '队标地址',
    `create_time` datetime     NOT NULL                COMMENT '创建时间',
    `update_time` datetime     NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '球队表';


-- ----------------------------------------------------------------------------
-- 球员
-- ----------------------------------------------------------------------------
CREATE TABLE `player` (
    `id`            int unsigned     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`          varchar(50)      NOT NULL                COMMENT '中文名',
    `name_en`       varchar(80)      DEFAULT ''              COMMENT '英文名',
    `team_id`       int unsigned     DEFAULT NULL            COMMENT '所属球队ID',
    `jersey_number` varchar(5)       DEFAULT ''              COMMENT '球衣号',
    `position`      varchar(10)      DEFAULT ''              COMMENT '位置：PG/SG/SF/PF/C',
    `height_cm`     smallint unsigned DEFAULT NULL           COMMENT '身高(cm)',
    `weight_kg`     smallint unsigned DEFAULT NULL           COMMENT '体重(kg)',
    `birth_date`    date             DEFAULT NULL            COMMENT '出生日期',
    `country`       varchar(30)      DEFAULT ''              COMMENT '国籍',
    `avatar_url`    varchar(255)     DEFAULT ''              COMMENT '头像地址',
    `is_active`     tinyint(1)       NOT NULL DEFAULT 1      COMMENT '是否现役：1是 0否',
    `create_time`   datetime         NOT NULL                COMMENT '创建时间',
    `update_time`   datetime         NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_player_team` (`team_id`),
    KEY `idx_player_name` (`name`),
    CONSTRAINT `fk_player_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '球员表';


-- ----------------------------------------------------------------------------
-- 赛季
-- ----------------------------------------------------------------------------
CREATE TABLE `season` (
    `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`        varchar(20)  NOT NULL                COMMENT '赛季名，如 2025-26',
    `start_date`  date         DEFAULT NULL            COMMENT '开始日期',
    `end_date`    date         DEFAULT NULL            COMMENT '结束日期',
    `is_current`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否当前赛季：1是 0否',
    `create_time` datetime     NOT NULL                COMMENT '创建时间',
    `update_time` datetime     NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_season_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '赛季表';


-- ----------------------------------------------------------------------------
-- 比赛
-- ----------------------------------------------------------------------------
CREATE TABLE `game` (
    `id`            int unsigned     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `season_id`     int unsigned     NOT NULL                COMMENT '赛季ID',
    `home_team_id`  int unsigned     NOT NULL                COMMENT '主队ID',
    `away_team_id`  int unsigned     NOT NULL                COMMENT '客队ID',
    `game_date`     datetime         NOT NULL                COMMENT '比赛时间',
    `home_score`    smallint unsigned DEFAULT NULL           COMMENT '主队得分',
    `away_score`    smallint unsigned DEFAULT NULL           COMMENT '客队得分',
    `status`        varchar(10)      NOT NULL DEFAULT '已结束' COMMENT '未开始/进行中/已结束',
    `venue`         varchar(80)      DEFAULT ''              COMMENT '比赛场馆',
    `create_time`   datetime         NOT NULL                COMMENT '创建时间',
    `update_time`   datetime         NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_game_season_date` (`season_id`, `game_date`),
    KEY `idx_game_home` (`home_team_id`),
    KEY `idx_game_away` (`away_team_id`),
    CONSTRAINT `fk_game_season` FOREIGN KEY (`season_id`)    REFERENCES `season` (`id`),
    CONSTRAINT `fk_game_home`   FOREIGN KEY (`home_team_id`) REFERENCES `team` (`id`),
    CONSTRAINT `fk_game_away`   FOREIGN KEY (`away_team_id`) REFERENCES `team` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '比赛表';


-- ----------------------------------------------------------------------------
-- 球员单场数据（核心事实表）
--   数据量最大，后续排行榜 / 进阶指标的聚合查询都打在这张表上
-- ----------------------------------------------------------------------------
CREATE TABLE `player_game_stat` (
    `id`          bigint unsigned    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `game_id`     int unsigned       NOT NULL                COMMENT '比赛ID',
    `player_id`   int unsigned       NOT NULL                COMMENT '球员ID',
    `team_id`     int unsigned       NOT NULL                COMMENT '所属球队ID',
    `is_starter`  tinyint(1)         NOT NULL DEFAULT 0      COMMENT '是否首发：1是 0否',
    `minutes`     decimal(4,1)       NOT NULL DEFAULT 0.0    COMMENT '上场时间(分钟)',
    `points`      smallint unsigned  NOT NULL DEFAULT 0      COMMENT '得分',
    `rebounds`    smallint unsigned  NOT NULL DEFAULT 0      COMMENT '篮板',
    `assists`     smallint unsigned  NOT NULL DEFAULT 0      COMMENT '助攻',
    `steals`      smallint unsigned  NOT NULL DEFAULT 0      COMMENT '抢断',
    `blocks`      smallint unsigned  NOT NULL DEFAULT 0      COMMENT '盖帽',
    `turnovers`   smallint unsigned  NOT NULL DEFAULT 0      COMMENT '失误',
    `fouls`       smallint unsigned  NOT NULL DEFAULT 0      COMMENT '犯规',
    `fgm`         smallint unsigned  NOT NULL DEFAULT 0      COMMENT '投篮命中数',
    `fga`         smallint unsigned  NOT NULL DEFAULT 0      COMMENT '投篮出手数',
    `fg3m`        smallint unsigned  NOT NULL DEFAULT 0      COMMENT '三分命中数',
    `fg3a`        smallint unsigned  NOT NULL DEFAULT 0      COMMENT '三分出手数',
    `ftm`         smallint unsigned  NOT NULL DEFAULT 0      COMMENT '罚球命中数',
    `fta`         smallint unsigned  NOT NULL DEFAULT 0      COMMENT '罚球出手数',
    `plus_minus`  smallint           NOT NULL DEFAULT 0      COMMENT '正负值',
    `create_time` datetime           NOT NULL                COMMENT '创建时间',
    `update_time` datetime           NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_player` (`game_id`, `player_id`),
    KEY `idx_pgs_player` (`player_id`),
    KEY `idx_pgs_team` (`team_id`),
    CONSTRAINT `fk_pgs_game`   FOREIGN KEY (`game_id`)   REFERENCES `game` (`id`),
    CONSTRAINT `fk_pgs_player` FOREIGN KEY (`player_id`) REFERENCES `player` (`id`),
    CONSTRAINT `fk_pgs_team`   FOREIGN KEY (`team_id`)   REFERENCES `team` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '球员单场数据表';


-- ----------------------------------------------------------------------------
-- 投篮事件（投篮热图的数据基础）
--
--   坐标系：以球场左底角为原点，单位为英尺(ft)
--     x：0 ~ 50   球场宽度方向（左右）
--     y：0 ~ 47   球场深度方向（0 = 底线，47 = 中线）
--   区域 zone 取值（8 类，热图按此聚合）：
--     篮下 / 油漆区 / 中距离 / 左底角三分 / 右底角三分 / 左翼三分 / 右翼三分 / 弧顶三分
-- ----------------------------------------------------------------------------
CREATE TABLE `shot` (
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `game_id`     int unsigned     NOT NULL                COMMENT '比赛ID',
    `player_id`   int unsigned     NOT NULL                COMMENT '球员ID',
    `quarter`     tinyint unsigned NOT NULL                COMMENT '节次：1-4，加时赛 5+',
    `game_clock`  varchar(10)      DEFAULT ''              COMMENT '本节剩余时间 mm:ss',
    `x`           decimal(5,2)     NOT NULL                COMMENT '横坐标(英尺) 0-50',
    `y`           decimal(5,2)     NOT NULL                COMMENT '纵坐标(英尺) 0-47，0为底线',
    `distance_ft` decimal(5,2)     DEFAULT NULL            COMMENT '出手距离(英尺)',
    `shot_type`   varchar(10)      NOT NULL                COMMENT '两分/三分/罚球',
    `made`        tinyint(1)       NOT NULL                COMMENT '是否命中：1是 0否',
    `zone`        varchar(20)      NOT NULL                COMMENT '区域，见文件头说明',
    `assisted_by` int unsigned     DEFAULT NULL            COMMENT '助攻人球员ID',
    `create_time` datetime         NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_shot_game_player` (`game_id`, `player_id`),
    KEY `idx_shot_zone` (`zone`),
    CONSTRAINT `fk_shot_game`   FOREIGN KEY (`game_id`)   REFERENCES `game` (`id`),
    CONSTRAINT `fk_shot_player` FOREIGN KEY (`player_id`) REFERENCES `player` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '投篮事件表';
