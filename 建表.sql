-- ============================================================
-- 水果识别系统 · 建表脚本 v0
-- 数据库：MySQL 8.0+（若实训强制 PostgreSQL，见文末说明）
-- 架构：方案 D —— Java 用 ProcessBuilder 调 python，实时识别
-- 已定：要登录 / 数据集 Fruits-360 / 4 张核心表
-- ============================================================

CREATE DATABASE IF NOT EXISTS `fruit_recognition`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `fruit_recognition`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `recognition_result`;
DROP TABLE IF EXISTS `image`;
DROP TABLE IF EXISTS `fruit_category`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)     NOT NULL                COMMENT '登录名',
  `password`    VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 哈希，禁止存明文',
  `nickname`    VARCHAR(50)     DEFAULT NULL            COMMENT '昵称',
  `avatar`      VARCHAR(255)    DEFAULT NULL            COMMENT '头像路径',
  `role`        TINYINT         NOT NULL DEFAULT 0      COMMENT '0=普通用户 1=管理员',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1=正常 0=禁用',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 水果类别字典（模型索引 ↔ 中文名的桥梁）
-- ------------------------------------------------------------
CREATE TABLE `fruit_category` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `name_cn`     VARCHAR(50)     NOT NULL                COMMENT '中文名，如「青苹果」',
  `name_en`     VARCHAR(50)     NOT NULL                COMMENT '英文名，= Fruits-360 文件夹名',
  `class_index` INT             NOT NULL                COMMENT '模型输出层索引，必须与训练时的类别顺序一致',
  `description` VARCHAR(255)    DEFAULT NULL            COMMENT '简介，答辩可讲',
  `cover_image` VARCHAR(255)    DEFAULT NULL            COMMENT '类别配图路径',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_index` (`class_index`),
  UNIQUE KEY `uk_name_en` (`name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水果类别字典';

-- ------------------------------------------------------------
-- 3. 图片记录
-- ------------------------------------------------------------
CREATE TABLE `image` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_name`   VARCHAR(255)    NOT NULL                COMMENT '原始文件名',
  `file_path`   VARCHAR(500)    NOT NULL                COMMENT '服务器存储路径',
  `file_size`   INT UNSIGNED    DEFAULT NULL            COMMENT '字节',
  `width`       INT             DEFAULT NULL,
  `height`      INT             DEFAULT NULL,
  `user_id`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '离线批处理的图为 NULL',
  `source`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1=用户上传 2=离线批处理',
  `upload_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_upload_time` (`upload_time`),
  CONSTRAINT `fk_image_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片记录';

-- ------------------------------------------------------------
-- 4. 识别结果（核心表：一张图 5 行，rank 1~5）
-- ------------------------------------------------------------
CREATE TABLE `recognition_result` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `image_id`      BIGINT UNSIGNED NOT NULL,
  `category_id`   INT UNSIGNED    NOT NULL,
  `confidence`    DECIMAL(5,4)    NOT NULL              COMMENT '置信度 0.0000~1.0000，如 0.8734',
  `rank`          TINYINT         NOT NULL              COMMENT '1~5 候选排名，rank=1 为最终结果',
  `model_version` VARCHAR(50)     DEFAULT NULL          COMMENT '哪个模型跑的',
  `cost_ms`       INT UNSIGNED    DEFAULT NULL          COMMENT '耗时毫秒，答辩可讲性能',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_image_rank` (`image_id`, `rank`),
  KEY `idx_category_id` (`category_id`),
  CONSTRAINT `fk_result_image`    FOREIGN KEY (`image_id`)    REFERENCES `image`(`id`)          ON DELETE CASCADE,
  CONSTRAINT `fk_result_category` FOREIGN KEY (`category_id`) REFERENCES `fruit_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识别结果';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号
-- ⚠️ 下面的哈希是占位符，不要直接用。生成方式：
--    Java: new BCryptPasswordEncoder().encode("你的密码")
--    Python: python -c "import bcrypt;print(bcrypt.hashpw(b'123456', bcrypt.gensalt()).decode())"
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '<待生成BCrypt哈希>', '管理员', 1);

-- 水果类别：**不要插示例数据**。
--
-- ⚠️ 2026-10-19 踩过的坑：曾经在这里插过 3 行「苹果/香蕉/橙子」的示例，
--    class_index 是编的（0/1/2），而模型真实返回的是 950/951/954。
--    结果：系统按 class_index 查不到 → 尝试插入 → 撞 name_en 唯一索引 →
--    Duplicate entry 'orange' for key 'fruit_category.uk_name_en'，上传直接 500。
--
-- 正确做法：**留空**，让 getOrCreateCategory() 自动收录；
--          等 Fruits-360 到位后，用脚本读文件夹名批量生成 131 行。
--
-- class_index 必须与训练时 datasets.ImageFolder 的类别顺序一致，手写必错。
-- 如果之前已经插过示例数据，执行下面这句清掉：
-- DELETE FROM `fruit_category` WHERE `description` LIKE '%示例数据%';

-- ============================================================
-- 常用查询（写接口时直接用）
-- ============================================================

-- 某张图的最终识别结果
-- SELECT i.id, i.file_path, fc.name_cn, r.confidence
-- FROM recognition_result r
-- JOIN fruit_category fc ON fc.id = r.category_id
-- JOIN image i ON i.id = r.image_id
-- WHERE r.image_id = ? AND r.rank = 1;

-- 某张图的前 5 个候选
-- SELECT r.rank, fc.name_cn, r.confidence
-- FROM recognition_result r
-- JOIN fruit_category fc ON fc.id = r.category_id
-- WHERE r.image_id = ? ORDER BY r.rank;

-- 各类别识别数量统计（答辩演示图表用）
-- SELECT fc.name_cn, COUNT(*) AS cnt
-- FROM recognition_result r
-- JOIN fruit_category fc ON fc.id = r.category_id
-- WHERE r.rank = 1
-- GROUP BY fc.name_cn ORDER BY cnt DESC;

-- ============================================================
-- ✅ 数据库已定：MySQL（2026-10-19 确认）
-- 实训资料未写死数据库，本人最熟悉 MySQL，故采用 MySQL 8。
-- （实训环境另装了 PostgreSQL 17 + pgvector，本项目不使用，不冲突。）
-- ============================================================
