package com.ssn.forum.service

import cats.implicits._
import cats.{~>, Applicative, Monad}
import com.ssn.forum._
import com.ssn.forum.db.{PersistentPost, Repository}
import com.ssn.forum.domain.Email

//Transactor on the service level allows to compose multiple DB ops in one transaction (to rollback f.e).
//Could be solved in a different way as well, so this is only one of options
final class PostService[DB[_], F[_]](
    posts: Repository.Posts[DB],
    postSecurity: Repository.PostsSecurity[DB],
    transact: DB ~> F
)(implicit A: Applicative[F], M: Monad[DB])
    extends PostService.Algebra[F] {
  override def createPost(topicId: TopicId, text: String, nickname: String, email: Email): F[(PostId, Token)] = {
    val dbOp = for {
      p     <- posts.insert(topicId, text, nickname, email)
      token <- postSecurity.insert(p.id)
    } yield p.id -> token
    transact(dbOp)
  }

  override def deletePost(topicId: TopicId, postId: PostId): F[Boolean] = {
    val dbOp = for {
      _       <- postSecurity.delete(postId)
      removed <- posts.delete(topicId, postId)
    } yield removed
    transact(dbOp)
  }

  override def editPost(topicId: TopicId, postId: PostId, text: String): F[Boolean] =
    transact(posts.edit(topicId, postId, text))

  override def isTokenValid(postId: PostId, token: Token): F[Boolean] =
    transact(postSecurity.get(postId).map {
      case Some(t) if t === token => true
      case _                      => false
    })

  override def listPostsAround(
      topicId: TopicId,
      postId: PostId,
      before: Int,
      after: Int
  ): F[List[PersistentPost]] =
    transact(posts.listPostsAround(topicId, postId, before, after))
}

object PostService {
  abstract class Algebra[F[_]] {
    def createPost(topicId: TopicId, text: String, nickname: String, email: Email): F[(PostId, Token)]
    def deletePost(topicId: TopicId, postId: PostId): F[Boolean]
    def editPost(topicId: TopicId, postId: PostId, text: String): F[Boolean]
    def listPostsAround(
        topicId: TopicId,
        postId: PostId,
        before: Int,
        after: Int
    ): F[List[PersistentPost]]
    def isTokenValid(postId: PostId, token: Token): F[Boolean]
  }
}
