USE read_history_wisely;

-- 1. 访问明细表 (用于查记录)
CREATE TABLE IF NOT EXISTS `tb_visit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) DEFAULT NULL COMMENT '访问IP',
  `province` varchar(64) DEFAULT NULL COMMENT '省份',
  `city` varchar(64) DEFAULT NULL COMMENT '城市',
  `page_path` varchar(255) DEFAULT NULL COMMENT '访问路径',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '设备信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站访问流水日志';

-- 2. 访问统计表 (用于存历史报表)
CREATE TABLE IF NOT EXISTS `tb_visit_stats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL COMMENT '统计日期',
  `province` varchar(64) NOT NULL COMMENT '省份',
  `city` varchar(64) DEFAULT '' COMMENT '城市',
  `pv_count` int(11) DEFAULT 0 COMMENT '访问量(PV)',
  `uv_count` int(11) DEFAULT 0 COMMENT '访客数(UV)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_date_area` (`stat_date`, `province`, `city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日地域访问统计';
