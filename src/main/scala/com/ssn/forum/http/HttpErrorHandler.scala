package com.ssn.forum.http

import cats.implicits._
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import com.ssn.forum.exceptions._
import com.ssn.forum.log
import com.ssn.forum.responses.ErrorResponse
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}

import scala.util.control.NonFatal

final class HttpErrorHandler[F[_]: Sync] extends Http4sDsl[F] with Http4sInstances {

  val authErrorHandler: AuthException => F[Response[F]] = ex => Forbidden(ErrorResponse(ex.msg))

  val httpErrorHandler: HttpException => F[Response[F]] = {
    case ex: CannotDecodeRequestBody => BadRequest(ErrorResponse(ex.cause.getOrElse(ex).getMessage))
    case PassThroughException        => InternalServerError(ErrorResponse("This shouldn't happen!"))
  }

  val serviceErrorHandler: ServiceException => F[Response[F]] = {
    case ex: PaginateLimitException => BadRequest(ErrorResponse(ex.getMessage))
    case BeforeOrAfterNegative      => BadRequest(ErrorResponse(BeforeOrAfterNegative.getMessage))
  }

  val handler: AppException => F[Response[F]] = {
    case ex: AuthException    => authErrorHandler(ex)
    case ex: HttpException    => httpErrorHandler(ex)
    case ex: ServiceException => serviceErrorHandler(ex)
  }

  def handle(routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req: Request[F] =>
    OptionT {
      routes.run(req).value.handleErrorWith { ex =>
        (ex match {
          case ex: AppException =>
            handler(ex)
          case NonFatal(ex) =>
            log.error[F]("Internal error occurred.", Some(ex)) *>
              InternalServerError(ErrorResponse(s"${ex.getMessage}"))
        }).map(Option(_))
      }
    }
  }

}
