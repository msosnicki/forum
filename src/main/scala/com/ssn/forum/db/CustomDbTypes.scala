package com.ssn.forum.db

import com.ssn.forum.domain.Email
import doobie.util.{Get, Put}

trait CustomDbTypes {
  implicit val emailGet: Get[Email] = Get[String].map(Email.apply(_).get)
  implicit val emailPut: Put[Email] = Put[String].contramap(_.value)
}
