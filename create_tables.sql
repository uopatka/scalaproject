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

CREATE TABLE book_entries (
  id SERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  isbn VARCHAR REFERENCES books(isbn),
  created_at TIMESTAMP WITHOUT TIME ZONE,
  status VARCHAR,
  pages_read INTEGER,
  alt_cover TEXT NOT NULL DEFAULT ''
);