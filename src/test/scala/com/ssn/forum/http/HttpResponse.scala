package com.ssn.forum.http

import java.util.UUID

import cats.{Functor, MonadError}
import com.softwaremill.sttp.{DeserializationError, Uri}
import com.ssn.forum.http.auth.SetAuthHeader
import io.circe.Error

sealed trait HttpResponse[+A] { self =>
  def code: Int
  def headers: Map[String, String]
  def asSuccess[F[_], A2 >: A](implicit ME: MonadError[F, Throwable]): F[HttpSuccess[A2]] = self match {
    case s @ HttpSuccess(_, _, _) => ME.pure(s)
    case resp                     => ME.raiseError(new RuntimeException(s"The response $resp was not a success."))
  }
}
case class HttpSuccess[+A](code: Int, headers: Map[String, String], body: A) extends HttpResponse[A] {
  def getAuthHeader: Option[UUID] = headers.get(SetAuthHeader).map(UUID.fromString)
}

object HttpSuccess {
  implicit val functor: Functor[HttpSuccess] = new Functor[HttpSuccess] {
    override def map[A, B](fa: HttpSuccess[A])(f: A => B): HttpSuccess[B] =
      HttpSuccess(fa.code, fa.headers, f(fa.body))
  }
}

case class HttpDeserializeError(code: Int, headers: Map[String, String], error: DeserializationError[Error])
    extends HttpResponse[Nothing]
case class HttpError(code: Int, headers: Map[String, String], uri: Uri, errorBody: String) extends HttpResponse[Nothing]
