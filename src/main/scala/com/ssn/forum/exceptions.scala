package com.ssn.forum

import com.ssn.forum.http.auth._
import pureconfig.error.ConfigReaderFailures

object exceptions {
  sealed abstract class AppException(val msg: String, ex: Option[Throwable]) extends Exception(msg, ex.orNull)

  case class ConfigException(reason: ConfigReaderFailures)
      extends AppException(
        s"Provided config has errors in it. Reasons: ${reason.toList.map(_.description).mkString(",")}",
        None
      )

  sealed abstract class HttpException(msg: String, ex: Option[Throwable]) extends AppException(msg, ex)

  sealed abstract class AuthException(msg: String, ex: Option[Throwable]) extends HttpException(msg, ex)

  sealed abstract class ServiceException(msg: String, ex: Option[Throwable]) extends HttpException(msg, ex)

  case class CannotDecodeRequestBody(clazz: Class[_], message: String, ex: Option[Throwable])
      extends HttpException(
        s"Cannot decode body of a request to ${clazz.getSimpleName} because of error $message",
        ex
      )

  case object TokenInvalidForPost                extends AuthException(s"Token provided is invalid for this post", None)
  case object TokenFormatInvalid                 extends AuthException(s"$AuthHeader is in a wrong format!", None)
  case object MissingAuthHeader                  extends AuthException(s"$AuthHeader is required to authenticate.", None)
  case class UnknownAuthException(ex: Throwable) extends AuthException("An unknown auth exception occured!", Some(ex))

  case class PaginateLimitException(given: Int, currentMax: Int)
      extends ServiceException(
        s"The given paginate limit $given is invalid. Expected a number 0 < x <= $currentMax",
        None
      )
  case object BeforeOrAfterNegative extends ServiceException("Before and after parameters cannot be negative!", None)

}
