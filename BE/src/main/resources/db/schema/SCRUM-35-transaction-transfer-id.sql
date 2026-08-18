-- SCRUM-35 link the withdrawal and deposit ledger rows of one transfer.

ALTER TABLE `transaction`
    ADD COLUMN transfer_id CHAR(36) NULL AFTER amount;

CREATE INDEX idx_transaction_transfer_id
    ON `transaction` (transfer_id);
