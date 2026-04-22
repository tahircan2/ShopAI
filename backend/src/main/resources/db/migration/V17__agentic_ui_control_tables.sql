-- V17 — Agentic UI Control: New tables and schema updates for AI-driven workflows

-- 1. agent_transactions
CREATE TABLE IF NOT EXISTS agent_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    transaction_type ENUM('CHECKOUT','CART_MODIFY','NAVIGATE','COUPON_APPLY','ORDER_QUERY','MULTI_STEP') NOT NULL,
    status ENUM('PENDING','IN_PROGRESS','AWAITING_APPROVAL','COMPLETED','FAILED','CANCELLED','ROLLED_BACK') NOT NULL DEFAULT 'PENDING',
    total_steps INT NOT NULL DEFAULT 0,
    completed_steps INT NOT NULL DEFAULT 0,
    failed_step INT,
    total_duration_ms BIGINT,
    tokens_used INT,
    error_message TEXT,
    result_order_number VARCHAR(20),
    feedback_score INT,
    feedback_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. agent_transaction_steps
CREATE TABLE IF NOT EXISTS agent_transaction_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type ENUM('VALIDATE_CART','CHECK_STOCK','APPLY_COUPON','SELECT_ADDRESS','CREATE_ORDER','CLEAR_CART','VALIDATE_PAYMENT','PRE_VALIDATE') NOT NULL,
    status ENUM('PENDING','IN_PROGRESS','COMPLETED','FAILED','ROLLED_BACK','SKIPPED') NOT NULL DEFAULT 'PENDING',
    step_description VARCHAR(500),
    request_data JSON,
    response_data JSON,
    error_message TEXT,
    duration_ms BIGINT,
    is_rollbackable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_step_transaction FOREIGN KEY (transaction_id) REFERENCES agent_transactions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. pending_approvals
CREATE TABLE IF NOT EXISTS pending_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT,
    approval_token VARCHAR(255) NOT NULL,
    plan_data JSON NOT NULL,
    plan_hash VARCHAR(100) NOT NULL,
    agent_type VARCHAR(50) NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_token (approval_token),
    CONSTRAINT fk_approval_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_approval_transaction FOREIGN KEY (transaction_id) REFERENCES agent_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. user_ai_preferences
CREATE TABLE IF NOT EXISTS user_ai_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    auto_approve_enabled BOOLEAN DEFAULT FALSE,
    auto_approve_max_amount DECIMAL(10,2) DEFAULT 500.00,
    auto_approve_categories JSON,
    use_default_address BOOLEAN DEFAULT TRUE,
    use_default_payment BOOLEAN DEFAULT TRUE,
    daily_transaction_limit INT DEFAULT 10,
    max_order_amount DECIMAL(10,2) DEFAULT 5000.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_pref_user (user_id),
    CONSTRAINT fk_ai_pref_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Add columns to existing tables (Confirmed they don't exist yet via DESCRIBE)
-- orders
ALTER TABLE orders 
    ADD COLUMN is_ai_created BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ai_agent_type VARCHAR(50),
    ADD COLUMN agent_transaction_id BIGINT;

-- carts
ALTER TABLE carts
    ADD COLUMN last_ai_interaction_at TIMESTAMP NULL,
    ADD COLUMN last_ai_agent VARCHAR(50);

-- audit_logs
ALTER TABLE audit_logs
    ADD COLUMN is_ai_action BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN agent_transaction_id BIGINT;

-- ai_conversations
ALTER TABLE ai_conversations
    ADD COLUMN active_transaction_id BIGINT,
    ADD COLUMN pending_approval_id BIGINT;

-- 6. Indexes
CREATE INDEX idx_agent_tx_user_id ON agent_transactions(user_id);
CREATE INDEX idx_agent_tx_session ON agent_transactions(session_id);
CREATE INDEX idx_agent_tx_status ON agent_transactions(status);
CREATE INDEX idx_agent_tx_created ON agent_transactions(created_at);
CREATE INDEX idx_agent_step_tx ON agent_transaction_steps(transaction_id);
CREATE INDEX idx_pending_approval_user_status ON pending_approvals(user_id, status);
CREATE INDEX idx_pending_approval_expires ON pending_approvals(expires_at);
