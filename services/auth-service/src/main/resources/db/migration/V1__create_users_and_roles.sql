CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE roles (
    name VARCHAR(32) PRIMARY KEY
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL REFERENCES roles(name),
    PRIMARY KEY (user_id, role_name)
);

INSERT INTO roles (name) VALUES ('CUSTOMER'), ('ADMIN');
