package com.ssn.forum.http

import cats.syntax.all._
import cats.effect.Sync
import com.ssn.forum.http.auth._
import com.ssn.forum.config.AppConfig
import com.ssn.forum.requests._
import com.ssn.forum.responses._
import com.ssn.forum.service._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

final class ForumHttp[F[_]](
    paginationLimit: Int,
    topicService: TopicService.Algebra[F],
    postService: PostService.Algebra[F]
) extends Http4sDsl[F]
    with Http4sInstances {
  import ForumHttp._

  private val limitValidator  = Validator.validateLimit(paginationLimit)(_)
  private val cropBeforeAfter = Validator.validateBeforeAndAfter(paginationLimit)(_, _)

  def openEndpoints(implicit S: Sync[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "topic" =>
      for {
        body            <- req.decodeAs[CreateTopicRequest]
        topicId         <- topicService.createTopic(body.subject)
        (postId, token) <- postService.createPost(topicId, body.text, body.nickname, body.email)
        resp            <- Created(TopicCreatedResponse(topicId, postId), Header(SetAuthHeader, token.toString))
      } yield resp

    case req @ POST -> Root / "topic" / LongVar(topicId) / "post" =>
      for {
        body            <- req.decodeAs[CreatePostRequest]
        (postId, token) <- postService.createPost(topicId, body.text, body.nickname, body.email)
        resp            <- Created(PostCreatedResponse(postId), Header(SetAuthHeader, token.toString))
      } yield resp
    case GET -> Root / "topic" :? LimitMatcher(limit) +& OffsetMatcher(offset) =>
      for {
        _    <- limitValidator(limit).liftTo[F]
        list <- topicService.listLastActive(offset, limit)
        resp <- Ok(TopicListResponse.from(list))
      } yield resp
    case GET -> Root / "topic" / LongVar(topicId) / "post" / LongVar(postId)
          :? BeforeMatcher(before) +& AfterMatcher(after) =>
      for {
        ba <- cropBeforeAfter(before, after).liftTo[F]
        list <- if (ba.cropped)
          postService.listPostsAroundWithAdjustment(topicId, postId, ba.before, ba.after)
        else
          postService.listPostsAround(topicId, postId, ba.before, ba.after)
        resp <- Ok(PostListResponse.from(list))
      } yield resp
  }

  def closedPostRoutes(implicit S: Sync[F]): AuthPostEndpoint[F] = AuthedRoutes.of {
    case authReq @ PUT -> Root / "topic" / LongVar(topicId) / "post" / LongVar(_) as postId =>
      for {
        body   <- authReq.req.decodeAs[EditPostRequest]
        edited <- postService.editPost(topicId, postId, body.text)
        resp   <- if (edited) Ok() else NotModified()
      } yield resp
    case DELETE -> Root / "topic" / LongVar(topicId) / "post" / LongVar(_) as postId =>
      for {
        deleted <- postService.deletePost(topicId, postId)
        resp    <- if (deleted) Ok() else NotModified()
      } yield resp
  }

}

object ForumHttp {
  object LimitMatcher  extends QueryParamDecoderMatcher[Int]("limit")
  object OffsetMatcher extends QueryParamDecoderMatcher[Long]("offset")

  object BeforeMatcher extends OptionalQueryParamDecoderMatcher[Int]("before")
  object AfterMatcher  extends OptionalQueryParamDecoderMatcher[Int]("after")
}
