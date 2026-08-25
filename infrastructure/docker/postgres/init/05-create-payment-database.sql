SELECT 'CREATE DATABASE novacommerce_payment' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='novacommerce_payment')\gexec
