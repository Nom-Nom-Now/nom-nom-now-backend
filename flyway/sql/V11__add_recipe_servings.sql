ALTER TABLE app.recipe
    ADD COLUMN IF NOT EXISTS servings INTEGER NOT NULL DEFAULT 1;

DO $$
BEGIN
    ALTER TABLE app.recipe
        ADD CONSTRAINT recipe_servings_chk CHECK (servings > 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
