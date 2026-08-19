-- SCRUM-38 transaction detail fields. Run once after the transaction table exists.

ALTER TABLE `transaction`
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' AFTER transaction_type,
    ADD COLUMN fee BIGINT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN channel VARCHAR(30) NOT NULL DEFAULT 'MOBILE' AFTER fee,
    ADD COLUMN reference_number VARCHAR(40) NULL AFTER transfer_id,
    ADD COLUMN description VARCHAR(100) NULL AFTER memo,
    ADD COLUMN canceled_at DATETIME(6) NULL AFTER description,
    ADD CONSTRAINT uk_transaction_reference_number UNIQUE (reference_number),
    ADD CONSTRAINT chk_transaction_fee_non_negative CHECK (fee >= 0);

UPDATE `transaction`
SET reference_number = CONCAT('HM-', DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), '-', LPAD(id, 8, '0'))
WHERE reference_number IS NULL;

UPDATE `transaction`
SET channel = CASE
        WHEN memo IN ('8월 연금', '8월 급여', '관리비', '통신비') THEN 'AUTO_TRANSFER'
        ELSE 'MOBILE'
    END,
    description = COALESCE(memo, counterparty_name),
    memo = CASE
        WHEN memo IN ('8월 연금', '8월 급여', '관리비', '통신비') THEN NULL
        ELSE memo
    END,
    status = 'COMPLETED',
    fee = 0;

ALTER TABLE `transaction`
    MODIFY COLUMN reference_number VARCHAR(40) NOT NULL;
