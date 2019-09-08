package com.ssn.forum.http

import cats.MonadError
import cats.implicits._
import org.http4s.HttpRoutes

object syntax {

  implicit def toHttpRoutesOps[F[_]: MonadError[?[_], Throwable]](v: HttpRoutes[F]): HttpRoutesOps[F] =
    new HttpRoutesOps[F](v)

  final class HttpRoutesOps[F[_]: MonadError[?[_], Throwable]](routes: HttpRoutes[F]) {
    def and(other: HttpRoutes[F]): HttpRoutes[F] =
      routes.recoverWith {
        case PassThroughException => other
      }
  }
}
