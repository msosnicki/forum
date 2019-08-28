package com.ssn.forum

import com.ssn.forum.http.auth._
import io.circe.DecodingFailure

object exceptions {
  sealed abstract class AppException(val msg: String, ex: Option[Throwable]) extends Exception(msg, ex.orNull)

  sealed abstract class AuthException(msg: String, ex: Option[Throwable]) extends AppException(msg, ex)
  case object TokenInvalidForPost                                         extends AuthException(s"Token provided is invalid for this post", None)
  case object TokenFormatInvalid                                          extends AuthException(s"$AuthHeader is in a wrong format!", None)
  case object MissingAuthHeader                                           extends AuthException(s"$AuthHeader is required to authenticate.", None)
  case class UnknownAuthException(ex: Throwable)                          extends AuthException("An unknown auth exception occured!", Some(ex))

  sealed abstract class HttpException(msg: String, ex: Option[Throwable]) extends AppException(msg, ex)
  case object PassThroughException                                        extends HttpException(s"Internal marker error", None)
  case class CannotDecodeRequestBody(clazz: Class[_], cause: Option[Throwable], ex: Throwable)
      extends HttpException(
        s"Cannot decode body of a request to ${clazz.getSimpleName} because of ${ex.getMessage}",
        Some(ex)
      )

  sealed abstract class ServiceException(msg: String, ex: Option[Throwable]) extends AppException(msg, ex)
  case class PaginateLimitException(given: Int, currentMax: Int)
      extends ServiceException(
        s"The given paginate limit $given is invalid. Expected a number 0 < x <= $currentMax",
        None
      )
  case object BeforeOrAfterNegative extends ServiceException("Before and after parameters cannot be negative!", None)
}
