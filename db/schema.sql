CREATE EXTENSION IF NOT EXISTS "citext";

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE topic (
  id BIGSERIAL PRIMARY KEY,
  subject varchar(200) NOT NULL
);

CREATE TABLE post (
  id BIGSERIAL PRIMARY KEY,
  topic_id BIGINT NOT NULL REFERENCES topic (id),
  text text NOT NULL,
  nickname varchar(20) NOT NULL,
  email citext NOT NULL,
  created_at timestamp NOT NULL DEFAULT NOW()
);

CREATE TABLE post_security (
  post_id BIGINT NOT NULL UNIQUE REFERENCES post(id),
  token UUID NOT NULL UNIQUE DEFAULT uuid_generate_v1()
);