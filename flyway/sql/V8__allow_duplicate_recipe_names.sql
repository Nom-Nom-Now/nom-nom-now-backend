BEGIN;

ALTER TABLE app.recipe
    DROP CONSTRAINT IF EXISTS recipe_name_key;

COMMIT;
