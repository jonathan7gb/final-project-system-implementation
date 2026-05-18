CREATE TYPE identity.user_role AS ENUM (
    'EMPLOYEE',
    'APPROVER',
    'STOREKEEPER',
    'ADMIN'
);

CREATE TABLE identity.users (
    id          UUID                NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100)        NOT NULL,
    email       VARCHAR(150)        NOT NULL,
    password    VARCHAR(255)        NOT NULL,
    role        identity.user_role  NOT NULL,
    active      BOOLEAN             NOT NULL DEFAULT true,
    created_at  TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT now(),

    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);
