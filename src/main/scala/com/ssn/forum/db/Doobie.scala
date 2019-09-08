package com.ssn.forum.db

import com.ssn.forum._
import com.ssn.forum.domain.{Email, Nickname, Subject, Topic}
import doobie._
import doobie.postgres.implicits.UuidType

object Doobie extends CustomDbTypes {
  object Topics extends Repository.Topics[ConnectionIO] {
    override def insert(subject: Subject): ConnectionIO[PersistentTopic] =
      Query.Topics
        .insert(subject)
        .withUniqueGeneratedKeys[PersistentTopic]("id", "subject")

    override def listLastActive(offset: Long, limit: Int): ConnectionIO[List[Topic.WithLastPostDate]] =
      Query.Topics
        .listLastActive(offset, limit)
        .to[List]

  }

  object Posts extends Repository.Posts[ConnectionIO] {
    override def insert(
        topicId: TopicId,
        text: String,
        nickname: Nickname,
        email: Email
    ): ConnectionIO[PersistentPost] =
      Query.Posts
        .insert(topicId, text, nickname, email)
        .withUniqueGeneratedKeys("id", "text", "nickname", "email", "created_at", "topic_id")

    override def edit(topicId: TopicId, postId: PostId, text: String): doobie.ConnectionIO[Boolean] =
      Query.Posts
        .edit(topicId, postId, text)
        .run
        .map(_ > 0)

    override def delete(topicId: TopicId, postId: PostId): ConnectionIO[Boolean] =
      Query.Posts
        .delete(topicId, postId)
        .run
        .map(_ > 0)

    override def listPostsAround[G[_]](
        topicId: TopicId,
        postId: PostId,
        before: Int,
        after: Int
    )(implicit B: Collection[PersistentPost, G]): ConnectionIO[G[PersistentPost]] =
      Query.Posts.listPostsAround(topicId, postId, before, after).to[G]
  }

  object PostsSecurity extends Repository.PostsSecurity[ConnectionIO] {
    override def insert(postId: PostId): ConnectionIO[Token] =
      Query.PostSecurity
        .insert(postId)
        .withUniqueGeneratedKeys("token")

    override def insert(postId: PostId, token: Token): ConnectionIO[Unit] =
      Query.PostSecurity
        .insert(postId, token)
        .withUniqueGeneratedKeys("post_id", "token")

    override def get(postId: PostId): ConnectionIO[Option[Token]] =
      Query.PostSecurity
        .get(postId)
        .option

    override def delete(postId: PostId): ConnectionIO[Boolean] =
      Query.PostSecurity
        .delete(postId)
        .run
        .map(_ > 0)
  }
}
