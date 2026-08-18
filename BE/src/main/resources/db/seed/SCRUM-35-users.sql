-- SCRUM-35 demo users
-- Login PINs: Kim Young-ja 111111, Kim Min-su 222222, Park Su-jin 333333.
-- Choi Seong-ho is a recipient-only persona and has no issued PIN.

START TRANSACTION;

INSERT INTO users (created_at, updated_at, ci, name, phone, pin_hash)
VALUES
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'HANMADI-DEMO-CI-001', '김영자', '01000000101',
     '$2a$10$zVSpE0vp8tmyEZxUcoA0fuwm9DHxVPrn97Nyxkro/reP1l4T5a5ze'),
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'HANMADI-DEMO-CI-002', '김민수', '01000000201',
     '$2a$10$Cn4qnhSZCwF20WCYgMEVx.5uMCpqID8m2dqyGBrOLOJfkxFbwDJy6'),
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'HANMADI-DEMO-CI-003', '박수진', '01000000301',
     '$2a$10$FXe5mZ9qYVwJBoi.cE7f1eYoI//R99/MwDmYLyfe7VE1FiHcJs/gO'),
    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'HANMADI-DEMO-CI-004', '최성호', '01000000401',
     '$2a$10$I0Gu5/8NEmi7ZGxT.29MvO1c8CEElk5A14FU0BooKwjrGYiuoa1kG')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone = VALUES(phone),
    pin_hash = VALUES(pin_hash),
    updated_at = CURRENT_TIMESTAMP(6);

COMMIT;
