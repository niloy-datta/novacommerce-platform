SELECT 'CREATE DATABASE novacommerce_order' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='novacommerce_order')\gexec
