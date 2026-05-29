CREATE TABLE IF NOT EXISTS users (
    id       INTEGER PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS lists (
    id          INTEGER PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    icon        VARCHAR(255),
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS fields (
    id            INTEGER PRIMARY KEY,
    list_id       BIGINT NOT NULL REFERENCES lists(id),
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(255) NOT NULL CHECK (type IN ('TEXT','NUMBER','RATING','DATE','BOOLEAN','IMAGE','VIDEO','LOCATION','OPTION','MULTI_OPTION')),
    required      BOOLEAN NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS field_choices (
    field_id BIGINT NOT NULL REFERENCES fields(id),
    choice   VARCHAR(255)
);
