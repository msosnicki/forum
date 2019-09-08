package com.ssn.forum.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

case class AppConfig(db: DbConfig, paginationLimit: Int)

object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader
}

case class DbConfig(url: String, user: String, password: String)

object DbConfig {
  implicit val reader: ConfigReader[DbConfig] = deriveReader
}
