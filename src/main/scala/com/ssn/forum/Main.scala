package com.ssn.forum

import cats.arrow.FunctionK
import cats.effect._
import cats.implicits._
import com.ssn.forum.config.{AppConfig, DbConfig}
import com.ssn.forum.db.Doobie
import com.ssn.forum.exceptions.PassThroughException
import com.ssn.forum.http.auth.HttpAuth
import com.ssn.forum.http.{ForumHttp, HttpErrorHandler}
import com.ssn.forum.service.{PostService, TopicService}
import doobie._
import doobie.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    run0[IO].use(_ => IO.never).as(ExitCode.Success)

  private[forum] def run0[F[_]: Sync: ConcurrentEffect: Timer: ContextShift]: Resource[F, Server[F]] = {
    import org.http4s.implicits._

    val config = AppConfig(DbConfig("jdbc:postgresql://localhost:5432/forum_app", "my_user", "my_pass"), 10)

    val xa = Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      config.db.url,
      config.db.user,
      config.db.password,
      ExecutionContexts.cachedThreadPool //TODO: fix
    )

    val transactor = new FunctionK[ConnectionIO, F] {
      override def apply[A](fa: doobie.ConnectionIO[A]): F[A] = fa.transact(xa)
    }

    val postService  = new PostService[ConnectionIO, F](Doobie.Posts, Doobie.PostsSecurity, transactor)
    val topicService = new TopicService[ConnectionIO, F](Doobie.Topics, transactor)
    val http         = new ForumHttp[F](config, topicService, postService)

    val open    = http.openEndpoints
    val handler = new HttpErrorHandler[F]
    val auth    = new HttpAuth[F](postService, handler)
    val closed  = auth.postAuth(http.closedPostRoutes)
    val routes = handler
      .handle(closed.recoverWith {
        case PassThroughException => open
      })
      .orNotFound
    BlazeServerBuilder[F]
      .bindHttp(8080, "localhost")
      .withHttpApp(routes)
      .resource
  }
  //    val cfg = pureconfig.loadConfig[AppConfig]
//    Resource
//      .liftF(AppModule.prod[F](cfg.toOption.get))
//      .flatMap(
//        d =>
//          BlazeServerBuilder[F]
//            .bindHttp(8080, "localhost")
//            .withHttpApp(d.endpoints)
//            .resource
//      )
}
