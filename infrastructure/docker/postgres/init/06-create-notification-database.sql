SELECT 'CREATE DATABASE novacommerce_notification' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='novacommerce_notification')\gexec
