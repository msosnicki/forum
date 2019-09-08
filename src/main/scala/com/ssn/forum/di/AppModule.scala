package com.ssn.forum.di

import cats.implicits._
import cats.arrow.FunctionK
import cats.effect.{Async, ContextShift, Resource}
import com.ssn.forum.config.AppConfig
import com.ssn.forum.db.Doobie
import com.ssn.forum.http.{ForumHttp, HttpErrorHandler, PassThroughException}
import com.ssn.forum.http.auth.HttpAuth
import com.ssn.forum.service.{PostService, TopicService}
import doobie._
import doobie.implicits._
import org.http4s.HttpApp
import com.ssn.forum.http.syntax._

trait AppModule[F[_]] {
  def routes: Resource[F, HttpApp[F]]
}

final class ProdModule[F[_]: Async: ContextShift](cfg: AppConfig) extends AppModule[F] {
  override val routes: Resource[F, HttpApp[F]] = {
    import org.http4s.implicits._

    for {
      dbPool <- ExecutionContexts.cachedThreadPool[F]
    } yield {
      val xa = Transactor.fromDriverManager[F](
        "org.postgresql.Driver",
        cfg.db.url,
        cfg.db.user,
        cfg.db.password,
        dbPool
      )
      val transactor = new FunctionK[ConnectionIO, F] {
        override def apply[A](fa: doobie.ConnectionIO[A]): F[A] = fa.transact(xa)
      }
      val postService  = new PostService[ConnectionIO, F](Doobie.Posts, Doobie.PostsSecurity, transactor)
      val topicService = new TopicService[ConnectionIO, F](Doobie.Topics, transactor)
      val http         = new ForumHttp[F](cfg.paginationLimit, topicService, postService)

      val open    = http.openEndpoints
      val handler = new HttpErrorHandler[F]
      val auth    = new HttpAuth[F](postService, handler)
      val closed  = auth.postAuth(http.closedPostRoutes)
      handler
        .handle(closed and open)
        .orNotFound
    }
  }
}
