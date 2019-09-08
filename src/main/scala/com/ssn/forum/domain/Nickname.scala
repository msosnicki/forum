package com.ssn.forum.domain

import io.circe.{Decoder, Encoder}

sealed abstract class Nickname extends {
  def value: String
}

object Nickname {

  val MaxLength = 20

  def apply(nickname: String): Option[Nickname] =
    if (nickname.length <= MaxLength) Some(NicknameImpl(nickname)) else None

  implicit val decoder: Decoder[Nickname] = Decoder[String].emap(
    str =>
      Nickname(str) match {
        case Some(v) => Right(v)
        case None    => Left("Invalid nickname")
      }
  )
  implicit val encoder: Encoder[Nickname] = Encoder[String].contramap(_.value)

  private case class NicknameImpl(value: String) extends Nickname

}
