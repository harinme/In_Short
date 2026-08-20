-- 저장 수취 계좌와 즐겨찾기를 분리한다.

ALTER TABLE contact
    ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE AFTER account_number;

ALTER TABLE contact
    ADD CONSTRAINT uk_contact_user_bank_account
        UNIQUE (user_id, bank_id, account_number);
