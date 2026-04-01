CREATE TABLE IF NOT EXISTS `tb_visit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) DEFAULT NULL COMMENT '访问IP',
  `province` varchar(64) DEFAULT NULL COMMENT '省份(用于地图)',
  `city` varchar(64) DEFAULT NULL COMMENT '城市',
  `page_path` varchar(255) DEFAULT NULL COMMENT '访问路径',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '设备信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站访问日志';