BEGIN;

ALTER TABLE app.category
    ADD COLUMN IF NOT EXISTS color TEXT;

ALTER TABLE app.recipe_category
    DROP COLUMN IF EXISTS color;

COMMIT;
