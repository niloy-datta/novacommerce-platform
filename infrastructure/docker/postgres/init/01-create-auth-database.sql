SELECT 'CREATE DATABASE novacommerce_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'novacommerce_auth')\gexec
