-- SCRUM-35 demo family relationships.

START TRANSACTION;

INSERT INTO user_relationships
    (created_at, updated_at, user_id, related_user_id, relationship_type)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), source_user.id, related_user.id, 'SON'
FROM users source_user
JOIN users related_user ON related_user.ci = 'HANMADI-DEMO-CI-002'
WHERE source_user.ci = 'HANMADI-DEMO-CI-001'
ON DUPLICATE KEY UPDATE
    relationship_type = VALUES(relationship_type),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO user_relationships
    (created_at, updated_at, user_id, related_user_id, relationship_type)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), source_user.id, related_user.id, 'MOTHER'
FROM users source_user
JOIN users related_user ON related_user.ci = 'HANMADI-DEMO-CI-001'
WHERE source_user.ci = 'HANMADI-DEMO-CI-002'
ON DUPLICATE KEY UPDATE
    relationship_type = VALUES(relationship_type),
    updated_at = CURRENT_TIMESTAMP(6);

COMMIT;
