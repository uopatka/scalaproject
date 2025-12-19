package models

case class User(id: Int, username: String, email: String,
                passwordHash: String)
