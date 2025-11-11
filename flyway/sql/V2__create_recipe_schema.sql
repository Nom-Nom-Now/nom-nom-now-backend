BEGIN;


DROP TABLE IF EXISTS app.recipe_category CASCADE;
DROP TABLE IF EXISTS app.recipe_component CASCADE;
DROP TABLE IF EXISTS app.recipe CASCADE;
DROP TABLE IF EXISTS app.ingredient CASCADE;
DROP TABLE IF EXISTS app.category CASCADE;


CREATE SCHEMA IF NOT EXISTS app;


CREATE TABLE IF NOT EXISTS app.ingredient
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS app.recipe
(
    id           BIGSERIAL PRIMARY KEY,
    name         TEXT NOT NULL UNIQUE,
    instructions TEXT,
    cooking_time INTEGER,
    CONSTRAINT recipe_cooking_time_chk
        CHECK (cooking_time IS NULL OR cooking_time >= 0)
);

CREATE TABLE IF NOT EXISTS app.category
(
    id         BIGSERIAL PRIMARY KEY,
    name       TEXT        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.recipe_component
(
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT NOT NULL REFERENCES app.recipe (id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES app.ingredient (id) ON DELETE CASCADE,
    quantity      NUMERIC(10, 2),
    unit          TEXT,
    CONSTRAINT recipe_component_quantity_chk
        CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT recipe_component_unique
        UNIQUE (recipe_id, ingredient_id)
);

CREATE TABLE IF NOT EXISTS app.recipe_category
(
    recipe_id   BIGINT NOT NULL REFERENCES app.recipe (id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES app.category (id) ON DELETE CASCADE,
    color       TEXT,
    PRIMARY KEY (recipe_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_rc_recipe ON app.recipe_component (recipe_id);
CREATE INDEX IF NOT EXISTS idx_rc_ingredient ON app.recipe_component (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_rk_category ON app.recipe_category (category_id);

COMMIT;
