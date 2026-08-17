CREATE TABLE IF NOT EXISTS system_missions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(255) NOT NULL,
    pillar VARCHAR(32) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    primary_attribute VARCHAR(32) NOT NULL,
    secondary_attribute VARCHAR(32) NULL,
    experience_reward INT NOT NULL,
    coin_reward INT NOT NULL,
    stat_reward INT NOT NULL,
    required_mission BIT NOT NULL,
    penalty_quest BIT NOT NULL,
    focus_minutes INT NOT NULL,
    active BIT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    attribute_type VARCHAR(32) NOT NULL,
    base_value INT NOT NULL,
    bonus_value INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_attribute_user_type UNIQUE (user_id, attribute_type),
    CONSTRAINT fk_user_attribute_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS user_quests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    system_mission_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    completed_at TIMESTAMP NULL,
    granted_experience INT NOT NULL DEFAULT 0,
    granted_coins INT NOT NULL DEFAULT 0,
    debuff_applied BIT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_quest_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_quest_mission FOREIGN KEY (system_mission_id) REFERENCES system_missions (id)
);

CREATE TABLE IF NOT EXISTS vault_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    description VARCHAR(160) NOT NULL,
    amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vault_transaction_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS focus_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    planned_minutes INT NOT NULL,
    completed_minutes INT NOT NULL DEFAULT 0,
    dungeon_clear BIT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    CONSTRAINT fk_focus_session_user FOREIGN KEY (user_id) REFERENCES users (id)
);
