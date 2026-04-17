BEGIN;

DROP TABLE IF EXISTS app.recipe_category;
DROP TABLE IF EXISTS app.category;

ALTER TABLE app.recipe
    ADD COLUMN IF NOT EXISTS categories TEXT;

CREATE INDEX IF NOT EXISTS idx_recipe_categories
    ON app.recipe USING GIN (to_tsvector('simple', categories));

COMMIT;