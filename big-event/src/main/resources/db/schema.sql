-- ============================================================================
--  篮球数据统计平台（hoops）数据库建表脚本 —— 基础三表
-- ============================================================================
--  环境要求：MySQL 8.0+（使用了 utf8mb4_0900_ai_ci 排序规则，5.7 不支持）
--  字符集  ：utf8mb4 / utf8mb4_0900_ai_ci
--  对应后端：hoops（Spring Boot 4.x + MyBatis + PageHelper）
--  对应实体：com.zhuyicheng.hoops.pojo.User / Category / Article
--
--  使用方法：
--      mysql -uroot -p < big-event/src/main/resources/db/schema.sql
--    或在 Navicat / IDEA Database 工具中打开本文件直接执行。
--
--  脚本结构：本文件由开发库导出还原（mysqldump --no-data），
--            与开发库结构完全一致，可直接用于重建 / 换机 / 答辩环境部署。
--            篮球业务表（team / player / season / game / player_game_stat / shot）
--            见同目录下的 schema-basketball.sql。
--
--  ⚠️⚠️ 危险提示 ⚠️⚠️
--  本脚本开头包含 DROP TABLE，重复执行会【清空 user / category / article
--  三张表的全部数据】。请仅在全新的空库上执行；对已有数据的库执行前
--  务必先备份：mysqldump -uroot -p --databases hoops > backup.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `hoops`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `hoops`;

-- 删除顺序不可颠倒：article 通过外键依赖 category 和 user，必须最先删除
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;


-- ----------------------------------------------------------------------------
-- 用户表
-- ----------------------------------------------------------------------------
CREATE TABLE `user` (
    `id`          int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `username`    varchar(20)  NOT NULL                COMMENT '用户名',
    `password`    varchar(100) NOT NULL                COMMENT '密码（BCrypt，兼容历史 MD5）',
    `nickname`    varchar(20)  DEFAULT ''              COMMENT '昵称',
    `email`       varchar(128) DEFAULT ''              COMMENT '邮箱',
    `user_pic`    varchar(128) DEFAULT ''              COMMENT '头像地址',
    `create_time` datetime     NOT NULL                COMMENT '创建时间',
    `update_time` datetime     NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';


-- ----------------------------------------------------------------------------
-- 文章分类表
-- ----------------------------------------------------------------------------
CREATE TABLE `category` (
    `id`             int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `category_name`  varchar(32)  NOT NULL                COMMENT '分类名称',
    `category_alias` varchar(32)  NOT NULL                COMMENT '分类别名',
    `create_user`    int unsigned NOT NULL                COMMENT '创建人ID',
    `create_time`    datetime     NOT NULL                COMMENT '创建时间',
    `update_time`    datetime     NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `fk_category_user` (`create_user`),
    CONSTRAINT `fk_category_user` FOREIGN KEY (`create_user`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章分类表';


-- ----------------------------------------------------------------------------
-- 文章表
-- ----------------------------------------------------------------------------
CREATE TABLE `article` (
    `id`          int unsigned   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `title`       varchar(30)    NOT NULL                COMMENT '文章标题',
    `content`     varchar(10000) NOT NULL                COMMENT '文章内容',
    `cover_img`   varchar(128)   NOT NULL                COMMENT '文章封面',
    `state`       varchar(10)    DEFAULT '草稿'          COMMENT '文章状态: 已发布/草稿',
    `category_id` int unsigned   NOT NULL                COMMENT '文章分类ID',
    `create_user` int unsigned   NOT NULL                COMMENT '创建人ID',
    `create_time` datetime       NOT NULL                COMMENT '创建时间',
    `update_time` datetime       NOT NULL                COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `fk_article_category` (`category_id`),
    KEY `fk_article_user` (`create_user`),
    CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
    CONSTRAINT `fk_article_user`     FOREIGN KEY (`create_user`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章表';


-- ============================================================================
--  【可选】结构优化建议 —— 默认不执行，需要时自行取消注释
-- ============================================================================
--
-- 1) 密码列已扩容（BCrypt 迁移已完成，保留说明供回溯）
--    说明：varchar(32) 是 MD5 十六进制的长度，而 BCrypt 哈希为 60 字符，
--          因此本脚本已直接声明为 varchar(100)。
--          历史 MD5 数据无需手动迁移：用户下次登录校验通过后，
--          UserController 会自动把哈希升级为 BCrypt 并写回数据库
--          （见 com.zhuyicheng.hoops.utils.PasswordEncoder#needsUpgrade）。
--
--
-- 2) 分类删除保护（已实现，保留说明供回溯）
--    说明：fk_article_category 未声明级联行为，删除「已被文章引用的分类」
--          会抛出 MySQL 1451 外键约束错误，原先被全局异常处理器吞成
--          「服务器繁忙，请稍候再试」。现已在 CategoryController#delete 中
--          捕获 DataIntegrityViolationException 并返回明确提示。
--          如需让数据库层面也更明确，可显式声明约束行为（MySQL 默认即 RESTRICT）：
--
-- ALTER TABLE `article` DROP FOREIGN KEY `fk_article_category`;
-- ALTER TABLE `article`
--     ADD CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`)
--     REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
--
--
-- 3) 列表查询性能优化
--    说明：ArticleMapper.list 使用 select * ，而 content 为 varchar(10000)，
--          单行最大约 40KB（utf8mb4），分页查询会白拉大量无用字段。
--          建议列表 SQL 显式列出需要的列（排除 content）：
--
-- ALTER TABLE `article` ADD INDEX `idx_user_state_category` (`create_user`, `state`, `category_id`);
--
-- ============================================================================
