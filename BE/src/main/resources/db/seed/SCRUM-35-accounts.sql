-- SCRUM-35 demo accounts. Requires SCRUM-35 users and fictional banks.

START TRANSACTION;

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '100-01-000001', u.name, '생활통장',
       2856500, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '100'
WHERE u.ci = 'HANMADI-DEMO-CI-001'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '200-01-000001', u.name, '연금통장',
       6300000, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '200'
WHERE u.ci = 'HANMADI-DEMO-CI-001'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '100-01-000002', u.name, '월급통장',
       4320000, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '100'
WHERE u.ci = 'HANMADI-DEMO-CI-002'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '300-01-000002', u.name, '저축통장',
       12500000, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '300'
WHERE u.ci = 'HANMADI-DEMO-CI-002'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '300-01-000003', u.name, '생활비통장',
       1780000, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '300'
WHERE u.ci = 'HANMADI-DEMO-CI-003'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO account
    (created_at, updated_at, account_number, holder, alias, balance, status, user_id, bank_id)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '100-01-000004', u.name, '성호반찬',
       9640000, 'ACTIVE', u.id, b.id
FROM users u
JOIN bank b ON b.code = '100'
WHERE u.ci = 'HANMADI-DEMO-CI-004'
ON DUPLICATE KEY UPDATE
    holder = VALUES(holder), alias = VALUES(alias), balance = VALUES(balance),
    status = VALUES(status), user_id = VALUES(user_id), bank_id = VALUES(bank_id),
    updated_at = CURRENT_TIMESTAMP(6);

COMMIT;
