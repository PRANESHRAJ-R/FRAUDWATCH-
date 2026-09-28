CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    owner_name VARCHAR(100) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    amount_threshold DECIMAL(19, 2) NULL,
    transaction_count_threshold INT NULL,
    window_seconds INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    timestamp DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_tx_sender FOREIGN KEY (sender_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_receiver FOREIGN KEY (receiver_id) REFERENCES accounts(id),
    INDEX idx_tx_sender_time (sender_id, timestamp),
    INDEX idx_tx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flagged_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    flagged_at DATETIME NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_flag_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_flag_rule FOREIGN KEY (rule_id) REFERENCES rules(id),
    CONSTRAINT uk_tx_rule UNIQUE (transaction_id, rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    outcome VARCHAR(20) NOT NULL,
    reviewer VARCHAR(100) NOT NULL,
    reason VARCHAR(255),
    reviewed_at DATETIME NOT NULL,
    CONSTRAINT fk_review_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
