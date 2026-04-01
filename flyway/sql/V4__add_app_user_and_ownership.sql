BEGIN;

CREATE TABLE IF NOT EXISTS app.app_user
(
    id         BIGSERIAL PRIMARY KEY,
    google_id  TEXT        NOT NULL UNIQUE,
    email      TEXT        NOT NULL,
    name       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE app.recipe
    ADD COLUMN owner_id BIGINT REFERENCES app.app_user (id);

CREATE INDEX IF NOT EXISTS idx_recipe_owner ON app.recipe (owner_id);

COMMIT;

