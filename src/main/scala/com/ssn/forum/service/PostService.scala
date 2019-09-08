package com.ssn.forum.service

import cats.implicits._
import cats.{~>, Applicative, Monad}
import com.ssn.forum._
import com.ssn.forum.db.{PersistentPost, Repository}
import com.ssn.forum.domain.{Email, Nickname}

//Transactor on the service level allows to compose multiple DB ops in one transaction (to rollback f.e).
//Could be solved in a different way as well, so this is only one of options
final class PostService[DB[_], F[_]](
    posts: Repository.Posts[DB],
    postSecurity: Repository.PostsSecurity[DB],
    transact: DB ~> F
)(implicit A: Applicative[F], M: Monad[DB])
    extends PostService.Algebra[F] {
  override def createPost(topicId: TopicId, text: String, nickname: Nickname, email: Email): F[(PostId, Token)] = {
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
  ): F[Vector[PersistentPost]] =
    transact(posts.listPostsAround(topicId, postId, before, after))
}

object PostService {
  abstract class Algebra[F[_]] {
    def createPost(topicId: TopicId, text: String, nickname: Nickname, email: Email): F[(PostId, Token)]
    def deletePost(topicId: TopicId, postId: PostId): F[Boolean]
    def editPost(topicId: TopicId, postId: PostId, text: String): F[Boolean]
    def listPostsAround(
        topicId: TopicId,
        postId: PostId,
        before: Int,
        after: Int
    ): F[Vector[PersistentPost]]

    /**
      * TODO: maybe even this is not required? Seems like a useful feature though.
      * Fixes the problem when number below limit is returned when rounding happen
      * i.e after rounding there are before = 7 after = 2 but only 5 before rows exits.
      * The two should be added up to the after
      */
    def listPostsAroundWithAdjustment(topicId: TopicId, postId: PostId, before: Int, after: Int)(
        implicit M: Monad[F]
    ): F[Vector[PersistentPost]] = {
      val size = before + after
      listPostsAround(topicId, postId, size, size).map(adjust(postId, _, before, after))
    }

    /**
      * I don't like that much how it's solved,
      * there are other ways to solve it (two hits to db for example or directly in db with a more complex query(?))
      * It fixes the "adjust if older records if there is not enough newer"
      * and "adjust if newer records if there is not enough older" tests in MainSpec pass.
      */
    private def adjust(
        postId: PostId,
        results: Vector[PersistentPost],
        before: Int,
        after: Int
    ): Vector[PersistentPost] = {
      val elemIndex                     = results.indexWhere(_.id == postId)
      val (resultsAfter, resultsBefore) = results.splitAt(elemIndex)
      val adjustBefore                  = Some(after - resultsAfter.size).filter(_ > 0)
      val adjustAfter                   = Some(before - resultsBefore.size + 1).filter(_ > 0)
      val adjustedAfter                 = resultsAfter.takeRight(adjustAfter.orEmpty + after)
      val adjustedBefore                = resultsBefore.take(adjustBefore.orEmpty + before + 1)
      adjustedAfter ++ adjustedBefore
    }
    def isTokenValid(postId: PostId, token: Token): F[Boolean]
  }
}
