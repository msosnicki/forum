package com.ssn.forum.http

import java.util.UUID

import cats.{Functor, MonadError}
import cats.syntax.all._
import cats.effect.{ContextShift, IO}
import io.circe.{Decoder, Encoder, Error}
import com.softwaremill.sttp.{MonadError => _, _}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.sttp.circe._
import com.ssn.forum.http.TestRequests.{TestCreatePostRequest, TestCreateTopicRequest, TestEditPostRequest}
import com.ssn.forum.{PostId, Token, TopicId}
import com.ssn.forum.http.auth.{AuthHeader, SetAuthHeader}
import com.ssn.forum.responses.{
  PostBasicResponse,
  PostCreatedResponse,
  PostListResponse,
  TopicBasicResponse,
  TopicCreatedResponse,
  TopicListResponse
}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.concurrent.ExecutionContext.global

object TestRequests extends TestRequests {

  case class TestCreateTopicRequest(subject: String, text: String, nickname: String, email: String)

  object TestCreateTopicRequest {
    implicit val encoder: Encoder[TestCreateTopicRequest] = deriveEncoder
  }

  case class TestCreatePostRequest(text: String, nickname: String, email: String)

  object TestCreatePostRequest {
    implicit val encoder: Encoder[TestCreatePostRequest] = deriveEncoder
  }

  case class TestEditPostRequest(text: String)

  object TestEditPostRequest {
    implicit val encoder: Encoder[TestEditPostRequest] = deriveEncoder
  }

}

trait TestRequests {

  implicit val cs: ContextShift[IO]             = IO.contextShift(global)
  implicit val client: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

  val createTopicRequest = TestCreateTopicRequest("Some subject", "Some main post", "some_user", "some@email.pl")
  val createPostRequest  = TestCreatePostRequest("Some post", "some_user", "some@email.pl")

  implicit val decoderTopicCreatedResponse: Decoder[TopicCreatedResponse] = deriveDecoder
  implicit val decoderPostCreatedResponse: Decoder[PostCreatedResponse]   = deriveDecoder
  implicit val decoderTopicBasicResponse: Decoder[TopicBasicResponse]     = deriveDecoder
  implicit val decoderTopicListResponse: Decoder[TopicListResponse]       = deriveDecoder
  implicit val decoderPostBasicResponse: Decoder[PostBasicResponse]       = deriveDecoder
  implicit val decoderPostListResponse: Decoder[PostListResponse]         = deriveDecoder

  def createTopic[A: BodySerializer](body: A = createTopicRequest): IO[HttpSuccess[TopicCreatedResponse]] =
    createTopicRaw(body).asSuccess

  def createTopicRaw[A: BodySerializer](body: A = createTopicRequest): IO[HttpResponse[TopicCreatedResponse]] = {
    val url = uri"http://127.0.0.1:8080/topic"
    val request = sttp
      .post(url)
      .body(body)
      .response(asJson[TopicCreatedResponse])
      .send()
    request.map(extractHeadersAndJsonBody(_, url))
  }

  def listTopics(offset: Long, limit: Int): IO[HttpSuccess[TopicListResponse]] =
    listTopicsRaw(offset, limit).asSuccess

  def listTopicsRaw(offset: Long, limit: Int): IO[HttpResponse[TopicListResponse]] = {
    val url = uri"http://127.0.0.1:8080/topic?offset=$offset&limit=$limit"
    val request = sttp
      .get(url)
      .response(asJson[TopicListResponse])
      .send()
    request.map(extractHeadersAndJsonBody(_, url))
  }

  def getPost(topicId: TopicId, postId: PostId): IO[HttpSuccess[Option[PostBasicResponse]]] =
    listPosts(topicId, postId).map(_.map(_.posts.find(_.id == postId)))

  def listPosts(
      topicId: TopicId,
      postId: PostId,
      before: Option[Int] = None,
      after: Option[Int] = None
  ): IO[HttpSuccess[PostListResponse]] = listPostsRaw(topicId, postId, before, after).asSuccess

  def listPostsRaw(
      topicId: TopicId,
      postId: PostId,
      before: Option[Int] = None,
      after: Option[Int] = None
  ): IO[HttpResponse[PostListResponse]] = {
    val url = uri"http://127.0.0.1:8080/topic/$topicId/post/$postId?before=$before&after=$after"
    val request = sttp
      .get(url)
      .response(asJson[PostListResponse])
      .send()
    request.map(extractHeadersAndJsonBody(_, url))
  }

  def createPost[A: BodySerializer](
      topicId: TopicId,
      body: A = createPostRequest
  ): IO[HttpSuccess[PostCreatedResponse]] =
    createPostRaw(topicId, body).asSuccess

  def deletePost(topicId: TopicId, postId: PostId, token: Token): IO[HttpSuccess[String]] =
    deletePostRaw(topicId, postId, token).asSuccess

  def deletePostRaw(topicId: TopicId, postId: PostId, token: Token): IO[HttpResponse[String]] = {
    val url = uri"http://localhost:8080/topic/$topicId/post/$postId"
    val request = sttp
      .delete(url)
      .header(AuthHeader, token.toString)
      .response(asString)
      .send()
    request.map(extractHeadersAndStringBody(_, url))
  }

  def createPostRaw[A: BodySerializer](
      topicId: TopicId,
      body: A = createPostRequest
  ): IO[HttpResponse[PostCreatedResponse]] = {
    val url = uri"http://localhost:8080/topic/$topicId/post"
    val request = sttp
      .post(url)
      .body(body)
      .response(asJson[PostCreatedResponse])
      .send()
    request.map(extractHeadersAndJsonBody(_, url))
  }

  def editPost(topicId: TopicId, postId: PostId, token: Token, text: String): IO[HttpSuccess[String]] =
    editPostRaw(topicId, postId, token, text).asSuccess

  def editPostRaw(topicId: TopicId, postId: PostId, token: Token, text: String): IO[HttpResponse[String]] = {
    val url = uri"http://localhost:8080/topic/$topicId/post/$postId"
    val request = sttp
      .put(url)
      .body(TestEditPostRequest(text))
      .header(AuthHeader, token.toString)
      .response(asString)
      .send()
    request.map(extractHeadersAndStringBody(_, url))
  }

  private def extractHeadersAndStringBody(r: Response[String], uri: Uri): HttpResponse[String] =
    extractHeadersAndBody(r, uri)(a => HttpSuccess(r.code, r.headers.toMap, a))

  private def extractHeadersAndJsonBody[A](
      r: Response[Either[DeserializationError[Error], A]],
      uri: Uri
  ): HttpResponse[A] =
    extractHeadersAndBody(r, uri)(
      decoded =>
        decoded
          .fold(ex => HttpDeserializeError(r.code, r.headers.toMap, ex), a => HttpSuccess(r.code, r.headers.toMap, a))
    )

  private def extractHeadersAndBody[A, B](r: Response[A], uri: Uri)(
      handleSuccess: A => HttpResponse[B]
  ): HttpResponse[B] =
    r.body
      .fold[HttpResponse[B]](
        ex => HttpError(r.code, r.headers.toMap, uri, ex),
        success => handleSuccess(success)
      )

  implicit def toHttpResponseOps[F[_]: MonadError[?[_], Throwable], A](v: F[HttpResponse[A]]): HttpResponseOps[F, A] =
    new HttpResponseOps[F, A](v)

  final class HttpResponseOps[F[_]: MonadError[?[_], Throwable], A](v: F[HttpResponse[A]]) {
    def asSuccess: F[HttpSuccess[A]] =
      v.flatMap(_.asSuccess[F, A])
  }

}
