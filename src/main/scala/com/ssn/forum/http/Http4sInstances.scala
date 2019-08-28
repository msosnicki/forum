package com.ssn.forum.http

import cats.effect.Sync
import cats.syntax.all._
import cats.{Applicative, MonadError}
import com.ssn.forum.exceptions.CannotDecodeRequestBody
import com.ssn.forum.http.Http4sDecoding.RequestOps
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, MessageBodyFailure, Request}

import scala.reflect.ClassTag

object Http4sInstances extends Http4sInstances

trait Http4sInstances extends CirceEncoders with CirceDecoders with Http4sDecoding

trait CirceEncoders {
  implicit def entityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait CirceDecoders {
  implicit def entityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
}

trait Http4sDecoding {
  implicit def toRequestOps[F[_]](r: Request[F]): RequestOps[F] = new RequestOps[F](r)
}

object Http4sDecoding {
  final class RequestOps[F[_]](val r: Request[F]) extends AnyVal {
    def decodeAs[A](implicit ME: MonadError[F, Throwable], D: EntityDecoder[F, A], C: ClassTag[A]): F[A] =
      r.as[A].adaptError {
        case ex: MessageBodyFailure => CannotDecodeRequestBody(C.runtimeClass, ex.cause, ex)
        case ex: Throwable          => CannotDecodeRequestBody(C.runtimeClass, None, ex)
      }
  }
}
