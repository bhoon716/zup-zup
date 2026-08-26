ALTER TABLE users
    ADD COLUMN oauth_issuer VARCHAR(255) NULL,
    ADD COLUMN oauth_subject VARCHAR(255) NULL,
    ADD CONSTRAINT uk_users_oauth_identity UNIQUE (oauth_issuer, oauth_subject),
    ADD CONSTRAINT chk_users_oauth_identity_pair CHECK (
        (oauth_issuer IS NULL AND oauth_subject IS NULL)
        OR (oauth_issuer IS NOT NULL AND oauth_subject IS NOT NULL)
    );
