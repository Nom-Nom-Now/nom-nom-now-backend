CREATE TABLE app.shopping_list (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    week_start DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shopping_list_owner FOREIGN KEY (owner_id) REFERENCES app.app_user(id) ON DELETE CASCADE
);

CREATE TABLE app.shopping_list_item (
    id BIGSERIAL PRIMARY KEY,
    shopping_list_id BIGINT NOT NULL,
    ingredient_name TEXT NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL,
    unit TEXT NOT NULL,
    CONSTRAINT fk_shopping_list_item_list FOREIGN KEY (shopping_list_id) REFERENCES app.shopping_list(id) ON DELETE CASCADE
);

CREATE INDEX idx_shopping_list_owner_created ON app.shopping_list(owner_id, created_at DESC);
CREATE INDEX idx_shopping_list_item_list ON app.shopping_list_item(shopping_list_id);
