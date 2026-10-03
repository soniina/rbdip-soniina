-- Apply after all instances read/write only first_name and last_name.
ALTER TABLE customers
    DROP COLUMN full_name;

ALTER TABLE orders DROP COLUMN customer_full_name;
