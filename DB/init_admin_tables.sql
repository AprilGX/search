-- 用户表
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `role` varchar(32) DEFAULT 'EDITOR' COMMENT '角色: EDITOR(编辑), ADMIN(超管)',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员表';

-- 简单的审核记录表
CREATE TABLE `sys_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_type` varchar(32) NOT NULL COMMENT '对象类型: EVENT, PERSON',
  `target_id` varchar(64) NOT NULL COMMENT '对象ID',
  `operator` varchar(64) NOT NULL COMMENT '操作人',
  `action` varchar(32) NOT NULL COMMENT '动作: APPROVE, REJECT',
  `comment` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- 预置一个超级管理员账号 (密码通常需要加密，这里暂时明文示例，实际开发请用 BCrypt)
-- 假设密码是 admin123
INSERT INTO `sys_user` (`username`, `password`, `role`) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt8ONt1uL.qUd0zLoMh.a.5jS9q', 'ADMIN');
