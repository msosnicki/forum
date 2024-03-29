package com.ssn.forum.db

import com.ssn.forum.domain.{Email, Nickname, Subject, Topic}
import com.ssn.forum._

object Repository {
  trait Topics[F[_]] {
    def insert(subject: Subject): F[PersistentTopic]
    def listLastActive(offset: Long, limit: Int): F[List[Topic.WithLastPostDate]]
  }

  trait Posts[F[_]] {
    def insert(topicId: TopicId, text: String, nickname: Nickname, email: Email): F[PersistentPost]
    def edit(topicId: TopicId, postId: PostId, text: String): F[Boolean]
    def delete(topicId: TopicId, postId: PostId): F[Boolean]
    def listPostsAround[G[_]](
        topicId: TopicId,
        postId: PostId,
        before: Int,
        after: Int
    )(implicit B: Collection[PersistentPost, G]): F[G[PersistentPost]]
  }

  trait PostsSecurity[F[_]] {
    def insert(postId: PostId): F[Token]
    def insert(postId: PostId, token: Token): F[Unit]
    def get(postId: PostId): F[Option[Token]]
    def delete(postId: PostId): F[Boolean]
  }
}
