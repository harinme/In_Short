-- 송금 실행 및 고객 재확인 시점의 FDS 판단 이력.

CREATE TABLE IF NOT EXISTS transfer_risk_assessment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    source_account_id BIGINT NOT NULL,
    transfer_id VARCHAR(36) NOT NULL,
    recipient_bank_code VARCHAR(10) NOT NULL,
    recipient_account_number VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    risk_signals VARCHAR(255) NOT NULL,
    customer_confirmed BOOLEAN NOT NULL,
    result VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_transfer_risk_transfer_id (transfer_id),
    INDEX idx_transfer_risk_source_created (source_account_id, created_at),
    CONSTRAINT fk_transfer_risk_source_account
        FOREIGN KEY (source_account_id) REFERENCES account (id),
    CONSTRAINT chk_transfer_risk_amount_positive CHECK (amount > 0)
);
