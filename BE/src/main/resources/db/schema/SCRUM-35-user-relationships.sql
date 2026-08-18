-- SCRUM-35 directional relationships between users.

CREATE TABLE IF NOT EXISTS user_relationships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    related_user_id BIGINT NOT NULL,
    relationship_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_relationships_direction UNIQUE (user_id, related_user_id),
    CONSTRAINT fk_user_relationships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_relationships_related_user
        FOREIGN KEY (related_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_relationships_not_self CHECK (user_id <> related_user_id),
    CONSTRAINT ck_user_relationships_type CHECK (
        relationship_type IN ('SON', 'DAUGHTER', 'MOTHER', 'FATHER', 'SPOUSE', 'SIBLING', 'OTHER')
    )
);
