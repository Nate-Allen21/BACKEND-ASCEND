START TRANSACTION;

DELETE FROM user_sessions;
DELETE FROM mission_completions;
DELETE FROM user_quests;
DELETE FROM focus_sessions;
DELETE FROM vault_transactions;
DELETE FROM user_attributes;
DELETE FROM users;

COMMIT;

ALTER TABLE user_sessions AUTO_INCREMENT = 1;
ALTER TABLE mission_completions AUTO_INCREMENT = 1;
ALTER TABLE user_quests AUTO_INCREMENT = 1;
ALTER TABLE focus_sessions AUTO_INCREMENT = 1;
ALTER TABLE vault_transactions AUTO_INCREMENT = 1;
ALTER TABLE user_attributes AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
