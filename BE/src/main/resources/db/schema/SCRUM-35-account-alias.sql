-- SCRUM-35 optional account alias.

ALTER TABLE account
    ADD COLUMN alias VARCHAR(30) NULL AFTER holder;
