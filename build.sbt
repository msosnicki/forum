import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.ssn",
    name := "forum",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    mainClass in assembly := Some("com.ssn.forum.Main"),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-feature",
      "-language:implicitConversions",
      "-language:existentials",
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    libraryDependencies ++= Seq(
      CatsEffect,
      Http4sBlazeServer,
      Http4sCirce,
      Http4sDsl,
      CirceCore,
      CirceGeneric,
      Doobie,
      DoobiePostgres,
      Postgres,
      Atto,
      Enumeratum,
      EnumeratumCirce,
      PureConfig,
      Logback,
      Guava,
      Sttp,
      SttpCats,
      SttpCirce,
      ScalaTest
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )
