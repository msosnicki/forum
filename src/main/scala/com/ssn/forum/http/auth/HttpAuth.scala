package com.ssn.forum.http.auth

import java.util.UUID

import cats.{ApplicativeError, MonadError}
import cats.implicits._
import cats.data.{EitherT, Kleisli, OptionT}
import com.ssn.forum.{PostId, Token}
import com.ssn.forum.exceptions._
import com.ssn.forum.http.{HttpErrorHandler, PassThroughException}
import com.ssn.forum.service.PostService
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, Headers, Request}
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString

import scala.util.Try

final class HttpAuth[F[_]](postService: PostService.Algebra[F], handler: HttpErrorHandler[F])(
    implicit ME: MonadError[F, Throwable]
) extends Http4sDsl[F] {
  def postAuth: AuthMiddleware[F, PostId] = {
    val authUser: Kleisli[F, Request[F], Either[AuthException, PostId]] = Kleisli(sealRequest)
    val onFailure: AuthedRoutes[AuthException, F] = Kleisli(
      req => OptionT.liftF(handler.authErrorHandler(req.authInfo))
    )
    AuthMiddleware(authUser, onFailure)
  }

  private def sealRequest(req: Request[F]): F[Either[AuthException, PostId]] = {
    def check(postId: PostId): F[Either[AuthException, PostId]] = {
      val validator = for {
        tokenString <- authHeaderString(req.headers)
        token       <- parseAuthHeader(tokenString)
        _           <- validateToken(postId, token)
      } yield postId
      validator.value
    }
    req match {
      case (PUT | DELETE) -> Root / "topic" / LongVar(_) / "post" / LongVar(postId) =>
        check(postId)
      case _ => ME.raiseError(PassThroughException)
    }
  }

  private def authHeaderString(headers: Headers): EitherT[F, AuthException, String] = {
    val a = headers
      .get(CaseInsensitiveString(AuthHeader))
      .fold[F[Either[AuthException, String]]](ME.pure(Left(MissingAuthHeader)))(h => ME.pure(h.value.asRight))
    EitherT(a)
  }

  private def parseAuthHeader(str: String): EitherT[F, AuthException, Token] =
    EitherT(Either.fromTry(Try(UUID.fromString(str))).leftMap[AuthException](_ => TokenFormatInvalid).pure[F])

  private def validateToken(postId: PostId, token: Token): EitherT[F, AuthException, Unit] =
    EitherT(postService.isTokenValid(postId, token).map(valid => if (valid) Right(()) else Left(TokenInvalidForPost)))

}
