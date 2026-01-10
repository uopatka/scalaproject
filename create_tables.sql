CREATE TABLE books (
  isbn VARCHAR PRIMARY KEY,
  title VARCHAR NOT NULL,
  author VARCHAR,
  publish_year INTEGER,
  pages INTEGER,
  cover VARCHAR
);

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR NOT NULL,
  password VARCHAR NOT NULL
);

CREATE TABLE entries (
  id SERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  entry_type VARCHAR NOT NULL,
  ref_id VARCHAR NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR,
  pages_read INTEGER DEFAULT 0,
  alt_cover TEXT NOT NULL DEFAULT '',
  finished_at DATE
);


CREATE INDEX idx_entries_type_ref ON entries (entry_type, ref_id);
CREATE INDEX idx_entries_user ON entries (user_id);

CREATE TABLE notes (
  id BIGSERIAL PRIMARY KEY,
  entry_id BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
  user_id BIGINT REFERENCES users(id),
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);