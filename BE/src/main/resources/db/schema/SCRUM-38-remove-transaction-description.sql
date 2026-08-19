-- SCRUM-38 remove duplicated description and keep memo optional.

UPDATE `transaction`
SET memo = NULL
WHERE memo IN ('8월 연금', '8월 급여', '관리비', '통신비');

ALTER TABLE `transaction`
    DROP COLUMN description;
