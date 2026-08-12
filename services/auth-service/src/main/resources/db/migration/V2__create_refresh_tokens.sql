CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoke_reason VARCHAR(64),
    replaced_by_token_id UUID REFERENCES refresh_tokens(id)
);

CREATE INDEX refresh_tokens_family_active_idx ON refresh_tokens (family_id, revoked_at);
CREATE INDEX refresh_tokens_user_active_idx ON refresh_tokens (user_id, revoked_at);
