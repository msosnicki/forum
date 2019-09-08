package com.ssn.forum.db

import com.ssn.forum.domain.{Email, Nickname, Subject}
import doobie.util.{Get, Put}

trait CustomDbTypes {
  implicit val emailGet: Get[Email] = Get[String].map(Email(_).get)
  implicit val emailPut: Put[Email] = Put[String].contramap(_.value)

  implicit val nicknameGet: Get[Nickname] = Get[String].map(Nickname(_).get)
  implicit val nicknamePut: Put[Nickname] = Put[String].contramap(_.value)

  implicit val subjectGet: Get[Subject] = Get[String].map(Subject(_).get)
  implicit val subjectPut: Put[Subject] = Put[String].contramap(_.value)
}
