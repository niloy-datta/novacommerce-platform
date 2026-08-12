ALTER TABLE products ADD COLUMN search_vector TSVECTOR;

CREATE OR REPLACE FUNCTION update_product_search_vector()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.short_description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.description, '')), 'C');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_products_search_vector
BEFORE INSERT OR UPDATE OF name, short_description, description ON products
FOR EACH ROW EXECUTE FUNCTION update_product_search_vector();

UPDATE products SET name = name;
CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);
