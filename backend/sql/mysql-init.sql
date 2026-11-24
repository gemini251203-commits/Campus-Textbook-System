-- MySQL 8.0 独立建库导引脚本
-- 在使用 mysql profile 时，可先执行本脚本创建数据库，随后将 SQL_INIT_MODE=always 自动初始化或手动依次执行 schema.sql 与 data.sql

CREATE DATABASE IF NOT EXISTS textbook_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE textbook_system;
