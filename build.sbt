val scala3Version = "3.3.0"
val http4sVersion = "0.23.23"

lazy val root = project
  .in(file("."))
  .settings(
    name := "esp32-cam-timelapse-client",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,
    scalacOptions += "-Ykind-projector:underscores",

    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "co.fs2" %% "fs2-core" % "3.7.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
