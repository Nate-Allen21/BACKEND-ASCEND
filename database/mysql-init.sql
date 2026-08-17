CREATE DATABASE IF NOT EXISTS ascend_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ascend_user'@'localhost' IDENTIFIED BY 'ascend_password';

GRANT ALL PRIVILEGES ON ascend_db.* TO 'ascend_user'@'localhost';

FLUSH PRIVILEGES;
