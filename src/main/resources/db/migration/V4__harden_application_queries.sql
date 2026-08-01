ALTER TABLE applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE companies
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_applications_user_status
    ON applications (user_id, status);

CREATE INDEX idx_applications_user_application_date
    ON applications (user_id, application_date);

CREATE INDEX idx_applications_user_created
    ON applications (user_id, created_at, id);
