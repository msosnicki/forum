package com.ssn.forum

import java.time.Instant

import com.ssn.forum.db.PersistentPost
import com.ssn.forum.domain.{Email, Nickname, Subject, Topic}
import io.circe._
import io.circe.generic.semiauto._

object responses {

  case class TopicCreatedResponse(topicId: TopicId, postId: PostId)

  object TopicCreatedResponse {
    implicit val encoder: Encoder[TopicCreatedResponse] = deriveEncoder
  }

  case class PostCreatedResponse(id: PostId)

  object PostCreatedResponse {
    implicit val encoder: Encoder[PostCreatedResponse] = deriveEncoder
  }

  case class ErrorResponse(message: String)

  case class TopicListResponse(topics: List[TopicBasicResponse])

  object TopicListResponse {
    def from(topics: List[Topic.WithLastPostDate]): TopicListResponse =
      TopicListResponse(topics.map(TopicBasicResponse.from))

    implicit val encoder: Encoder[TopicListResponse] = deriveEncoder
  }

  case class PostListResponse(posts: Seq[PostBasicResponse])

  object PostListResponse {
    def from(posts: Seq[PersistentPost]): PostListResponse =
      PostListResponse(posts.map(PostBasicResponse.from))

    implicit val encoder: Encoder[PostListResponse] = deriveEncoder
  }

  case class PostBasicResponse(id: PostId, nickname: Nickname, email: Email, text: String, createdAt: Instant)

  object PostBasicResponse {
    def from(post: PersistentPost): PostBasicResponse =
      PostBasicResponse(post.id, post.entity.nickname, post.entity.email, post.entity.text, post.entity.createdAt)

    implicit val encoder: Encoder[PostBasicResponse] = deriveEncoder
  }

  case class TopicBasicResponse(id: TopicId, subject: Subject, lastPost: Instant)

  object TopicBasicResponse {
    def from(topic: Topic.WithLastPostDate): TopicBasicResponse =
      TopicBasicResponse(topic.topic.id, topic.topic.entity.subject, topic.lastPostTime)

    implicit val encoder: Encoder[TopicBasicResponse] = deriveEncoder

  }

  object ErrorResponse {
    implicit val encoder: Encoder[ErrorResponse] = deriveEncoder
  }

}
