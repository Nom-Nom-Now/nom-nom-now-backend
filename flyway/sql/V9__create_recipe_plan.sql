CREATE TABLE app.recipe_plan (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_plan_owner FOREIGN KEY (owner_id) REFERENCES app.app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_plan_recipe FOREIGN KEY (recipe_id) REFERENCES app.recipe(id) ON DELETE CASCADE,
    CONSTRAINT uq_recipe_plan_owner_date UNIQUE (owner_id, plan_date)
);

CREATE INDEX idx_recipe_plan_owner_date ON app.recipe_plan(owner_id, plan_date);
