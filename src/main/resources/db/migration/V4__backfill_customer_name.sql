-- Apply after all instances write both full_name and the new name fields.
UPDATE customers
SET first_name = CASE
        WHEN strpos(full_name, ' ') = 0 THEN full_name
        ELSE left(full_name, strpos(full_name, ' ') - 1)
    END,
    last_name = CASE
        WHEN strpos(full_name, ' ') = 0 THEN NULL
        ELSE substring(full_name FROM strpos(full_name, ' ') + 1)
    END
WHERE first_name IS NULL;

ALTER TABLE customers
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN full_name DROP NOT NULL;

ALTER TABLE customers
    DROP CONSTRAINT uk_customers_contact_details,
    ADD CONSTRAINT uk_customers_name_contact_details
        UNIQUE NULLS NOT DISTINCT (first_name, last_name, address, phone);
