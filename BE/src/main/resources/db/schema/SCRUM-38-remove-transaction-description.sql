-- SCRUM-38 remove duplicated description and keep memo optional.

UPDATE `transaction`
SET memo = NULL
WHERE counterparty_name IN
      ('연금공단', '한마디주식회사', '한마디아파트 관리사무소', '한마디통신');

ALTER TABLE `transaction`
    DROP COLUMN description;
