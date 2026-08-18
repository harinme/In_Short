-- SCRUM-35 fictional banks for demo data.

START TRANSACTION;

INSERT INTO bank (created_at, updated_at, name, code)
VALUES
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '한마디은행', '100'),
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '다온은행', '200'),
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), '누리은행', '300')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    updated_at = CURRENT_TIMESTAMP(6);

COMMIT;
