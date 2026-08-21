-- 기존 위험 확인 이력에 5분 만료 및 1회 사용 감사 시각을 추가한다.

ALTER TABLE transfer_risk_assessment
    ADD COLUMN IF NOT EXISTS expires_at DATETIME(6) NULL AFTER customer_confirmed,
    ADD COLUMN IF NOT EXISTS confirmed_at DATETIME(6) NULL AFTER expires_at,
    ADD COLUMN IF NOT EXISTS consumed_at DATETIME(6) NULL AFTER confirmed_at,
    ADD COLUMN IF NOT EXISTS confirmation_attempts INT NOT NULL DEFAULT 0 AFTER consumed_at,
    ADD COLUMN IF NOT EXISTS retention_until DATETIME(6) NULL AFTER confirmation_attempts;

UPDATE transfer_risk_assessment
SET retention_until = DATE_ADD(created_at, INTERVAL 5 YEAR)
WHERE retention_until IS NULL;

ALTER TABLE transfer_risk_assessment
    MODIFY retention_until DATETIME(6) NOT NULL;
