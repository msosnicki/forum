package com.ssn.forum.domain

import io.circe.{Decoder, DecodingFailure, Encoder}

abstract class Subject {
  def value: String
}

object Subject {
  private val MaxLength = 200

  def apply(str: String): Option[Subject] =
    if (str.length <= MaxLength) Some(SubjectImpl(str)) else None

  implicit val encoder: Encoder[Subject] = Encoder[String].contramap(_.value)
  implicit val decoder: Decoder[Subject] = Decoder[String].emap(
    str =>
      Subject(str) match {
        case None    => Left("Invalid subject")
        case Some(v) => Right(v)
      }
  )

  private case class SubjectImpl(value: String) extends Subject
}
