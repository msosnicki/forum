package com.ssn.forum

import cats.effect._
import cats.implicits._
import com.ssn.forum.config.AppConfig
import com.ssn.forum.di.{AppModule, ProdModule}
import com.ssn.forum.exceptions.ConfigException
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      cfg <- pureconfig.loadConfig[AppConfig].leftMap(ConfigException).liftTo[IO]
      module = new ProdModule[IO](cfg)
      _ <- run0[IO](module).use(_ => IO.never)
    } yield ExitCode.Success

  private[forum] def run0[F[_]: Sync: ConcurrentEffect: Timer: ContextShift](
      module: AppModule[F]
  ): Resource[F, Server[F]] =
    module.routes.flatMap(
      routes =>
        BlazeServerBuilder[F]
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(routes)
          .resource
    )
}
