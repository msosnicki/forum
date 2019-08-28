package com.ssn.forum.config

case class AppConfig(db: DbConfig, limit: Int)

case class DbConfig(url: String, user: String, password: String)
