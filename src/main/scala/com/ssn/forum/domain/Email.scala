package com.ssn.forum.domain

import io.circe.{Decoder, Encoder}

trait Email {
  def value: String
}

object Email {

  def apply(str: String): Option[Email] = str match {
    case EmailRegex(email) => Some(EmailImpl(email))
    case _                 => None
  }

  implicit val encoder: Encoder[Email] = Encoder[String].contramap(_.value)
  implicit val decoder: Decoder[Email] = Decoder[String].emap(
    str =>
      Email(str) match {
        case Some(v) => Right(v)
        case None    => Left("Invalid email address")
      }
  )

  private case class EmailImpl(value: String) extends Email

  //:D
  //https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
  private val EmailRegex =
    """(^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$)""".r

}
