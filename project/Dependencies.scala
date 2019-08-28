import sbt._

object Dependencies {

  object Version {
    val CatsEffect = "1.3.1"
    val Http4s     = "0.20.6"
    val Circe      = "0.11.1"
    val Doobie     = "0.7.0"
    val Postgres   = "9.1-901-1.jdbc4"
    val Atto       = "0.6.5"
    val Enumeratum = "1.5.13"
    val Logback    = "1.2.3"
    val PureConfig = "0.11.1"
    val Guava      = "14.0"
    val Sttp       = "1.6.4"
    val ScalaTest  = "3.0.7"
    val ScalaCheck = "1.14.0"
  }

  lazy val CatsEffect = "org.typelevel" %% "cats-effect" % Version.CatsEffect

  lazy val Http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % Version.Http4s
  lazy val Http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % Version.Http4s
  lazy val Http4sCirce       = "org.http4s" %% "http4s-circe"        % Version.Http4s
  lazy val Http4sDsl         = "org.http4s" %% "http4s-dsl"          % Version.Http4s

  lazy val CirceCore    = "io.circe" %% "circe-core"    % Version.Circe
  lazy val CirceGeneric = "io.circe" %% "circe-generic" % Version.Circe

  lazy val Doobie         = "org.tpolecat" %% "doobie-core"     % Version.Doobie
  lazy val DoobiePostgres = "org.tpolecat" %% "doobie-postgres" % Version.Doobie

  lazy val Postgres = "postgresql" % "postgresql" % Version.Postgres

  lazy val Atto = "org.tpolecat" %% "atto-core" % Version.Atto

  lazy val Enumeratum      = "com.beachape" %% "enumeratum"       % Version.Enumeratum
  lazy val EnumeratumCirce = "com.beachape" %% "enumeratum-circe" % Version.Enumeratum

  lazy val PureConfig = "com.github.pureconfig" %% "pureconfig" % Version.PureConfig

  lazy val Logback = "ch.qos.logback" % "logback-classic" % Version.Logback

  lazy val Guava = "com.google.guava" % "guava" % Version.Guava

  lazy val Sttp      = "com.softwaremill.sttp" %% "core"                           % Version.Sttp % Test
  lazy val SttpCats  = "com.softwaremill.sttp" %% "async-http-client-backend-cats" % Version.Sttp % Test
  lazy val SttpCirce = "com.softwaremill.sttp" %% "circe"                          % Version.Sttp % Test

  lazy val ScalaTest = "org.scalatest" %% "scalatest" % Version.ScalaTest % Test

  lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck % Test

}
