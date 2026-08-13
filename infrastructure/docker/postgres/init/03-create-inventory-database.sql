SELECT 'CREATE DATABASE novacommerce_inventory'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novacommerce_inventory')\gexec
