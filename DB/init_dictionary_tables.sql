USE `read_history_wisely`;

-- ========================================================
-- 表 1: 搜索词待处理任务表 (Search Term Task)
-- 作用: 记录用户搜索的高频词，作为 AI 分析的候选池
-- 状态机: 0-待处理, 1-已收录, 2-已忽略(不是词)
-- ========================================================
DROP TABLE IF EXISTS `search_term_task`;
CREATE TABLE `search_term_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `term` varchar(255) NOT NULL COMMENT '搜索词 (去重核心)',
  `hit_count` int(11) DEFAULT '1' COMMENT '搜索频次/热度',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0-待处理, 1-已收录, 2-已忽略',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '首次搜索时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term` (`term`) USING BTREE COMMENT '搜索词唯一索引',
  KEY `idx_status_count` (`status`,`hit_count`) USING BTREE COMMENT '用于定时任务快速捞取'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索词挖掘任务表';

-- ========================================================
-- 表 2: 动态词典存储表 (Dictionary Store)
-- 作用: 存储 AI 确认后的专有名词及同义词规则，供 IK 和 ES 使用
-- ========================================================
DROP TABLE IF EXISTS `dictionary_store`;
CREATE TABLE `dictionary_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` varchar(255) NOT NULL COMMENT '核心词汇 (存入 IK 词典)',
  `synonym_rule` varchar(512) DEFAULT NULL COMMENT '同义词规则 (例如: 天下大赦 => 天下大赦, 大赦)',
  `type` varchar(50) DEFAULT 'history' COMMENT '词汇类型 (history/person/place...)',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用: 1-启用, 0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`) USING BTREE COMMENT '词汇唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI动态词典库';
