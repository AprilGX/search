USE `read_history_wisely`;

-- 修改现有表的排序规则为 utf8mb4_general_ci

ALTER TABLE `sys_user` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `sys_audit_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `search_term_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `dictionary_store` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `tb_visit_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `tb_visit_stats` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
