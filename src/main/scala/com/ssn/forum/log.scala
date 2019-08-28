package com.ssn.forum

import cats.effect.Sync
import org.slf4j.LoggerFactory

object log {

  private val logger = LoggerFactory.getLogger("ForumLog")

  def error[F[_]: Sync](msg: String, ex: Option[Throwable] = None): F[Unit] =
    Sync[F].delay(ex.fold(logger.error(msg))(err => logger.error(msg, err)))
}
