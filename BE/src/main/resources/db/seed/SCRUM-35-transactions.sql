-- SCRUM-35 demo transaction history as account-ledger rows.

START TRANSACTION;

INSERT INTO `transaction`
    (created_at, updated_at, account_id, counterparty_bank_id, transaction_type,
     amount, transfer_id, balance_after, counterparty_name, counterparty_account, memo)
SELECT seed.created_at, seed.created_at, account.id, counterparty_bank.id,
       seed.transaction_type, seed.amount, seed.transfer_id, seed.balance_after,
       seed.counterparty_name, seed.counterparty_account, seed.memo
FROM (
    SELECT TIMESTAMP('2026-07-15 10:00:00') AS created_at, '100-01-000001' AS account_number,
           '300' AS counterparty_bank_code, 'WITHDRAW' AS transaction_type, 32000 AS amount,
           '35000000-0000-0000-0000-000000000001' AS transfer_id, 2818500 AS balance_after,
           '박수진' AS counterparty_name, '300-01-000003' AS counterparty_account, '모임비' AS memo
    UNION ALL
    SELECT TIMESTAMP('2026-07-15 10:00:00'), '300-01-000003', '100', 'DEPOSIT', 32000,
           '35000000-0000-0000-0000-000000000001', 1748000,
           '김영자', '100-01-000001', '모임비'
    UNION ALL
    SELECT TIMESTAMP('2026-07-28 14:30:00'), '300-01-000003', '100', 'WITHDRAW', 18000,
           '35000000-0000-0000-0000-000000000002', 1730000,
           '김영자', '100-01-000001', '장보기 정산'
    UNION ALL
    SELECT TIMESTAMP('2026-07-28 14:30:00'), '100-01-000001', '300', 'DEPOSIT', 18000,
           '35000000-0000-0000-0000-000000000002', 2836500,
           '박수진', '300-01-000003', '장보기 정산'
    UNION ALL
    SELECT TIMESTAMP('2026-08-01 09:00:00'), '200-01-000001', '100', 'DEPOSIT', 650000,
           NULL, 6300000, '연금공단', '100-99-000001', '8월 연금'
    UNION ALL
    SELECT TIMESTAMP('2026-08-01 09:10:00'), '100-01-000002', '100', 'DEPOSIT', 3500000,
           NULL, 4800000, '한마디주식회사', '100-99-000002', '8월 급여'
    UNION ALL
    SELECT TIMESTAMP('2026-08-03 11:00:00'), '100-01-000001', '300', 'WITHDRAW', 50000,
           '35000000-0000-0000-0000-000000000003', 2786500,
           '박수진', '300-01-000003', '회비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-03 11:00:00'), '300-01-000003', '100', 'DEPOSIT', 50000,
           '35000000-0000-0000-0000-000000000003', 1780000,
           '김영자', '100-01-000001', '회비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-05 18:20:00'), '100-01-000002', '100', 'WITHDRAW', 300000,
           '35000000-0000-0000-0000-000000000004', 4500000,
           '김영자', '100-01-000001', '생활비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-05 18:20:00'), '100-01-000001', '100', 'DEPOSIT', 300000,
           '35000000-0000-0000-0000-000000000004', 3086500,
           '김민수', '100-01-000002', '생활비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-08 08:30:00'), '100-01-000001', '300', 'WITHDRAW', 185000,
           NULL, 2901500, '한마디아파트 관리사무소', '300-99-000101', '관리비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-10 08:30:00'), '100-01-000001', '200', 'WITHDRAW', 45000,
           NULL, 2856500, '한마디통신', '200-99-000201', '통신비'
    UNION ALL
    SELECT TIMESTAMP('2026-08-12 20:00:00'), '100-01-000002', '300', 'WITHDRAW', 180000,
           '35000000-0000-0000-0000-000000000005', 4320000,
           '김민수', '300-01-000002', '저축'
    UNION ALL
    SELECT TIMESTAMP('2026-08-12 20:00:00'), '300-01-000002', '100', 'DEPOSIT', 180000,
           '35000000-0000-0000-0000-000000000005', 12500000,
           '김민수', '100-01-000002', '저축'
) seed
JOIN account ON account.account_number = seed.account_number
JOIN bank counterparty_bank ON counterparty_bank.code = seed.counterparty_bank_code
LEFT JOIN `transaction` existing
    ON existing.account_id = account.id
    AND (
        (seed.transfer_id IS NOT NULL AND existing.transfer_id = seed.transfer_id)
        OR (
            seed.transfer_id IS NULL
            AND existing.created_at = seed.created_at
            AND existing.amount = seed.amount
            AND existing.memo = seed.memo
        )
    )
WHERE existing.id IS NULL;

COMMIT;
