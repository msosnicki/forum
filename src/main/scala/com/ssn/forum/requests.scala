package com.ssn.forum

import com.ssn.forum.domain.Email
import io.circe._
import io.circe.generic.semiauto._

object requests {

  case class CreateTopicRequest(subject: String, text: String, nickname: String, email: Email)

  object CreateTopicRequest {
    implicit val decoder: Decoder[CreateTopicRequest] = deriveDecoder
  }

  case class CreatePostRequest(text: String, nickname: String, email: Email)

  object CreatePostRequest {
    implicit val decoder: Decoder[CreatePostRequest] = deriveDecoder
  }

  case class EditPostRequest(text: String)

  object EditPostRequest {
    implicit val decoder: Decoder[EditPostRequest] = deriveDecoder
  }
}
