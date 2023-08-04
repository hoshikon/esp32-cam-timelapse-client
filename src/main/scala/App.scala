import ImageServerClient.Resolution
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.either.catsSyntaxEither

import scala.concurrent.duration.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.Instant

object App extends IOApp.Simple:
  override def run: IO[Unit] =
    val configOrError = Config
      .fromEnv(sys.env)
      .toEither
      .leftMap(errors => new RuntimeException(s"Failed to read configs:${errors.toList.mkString("\n", "\n", "\n")}"))
    for
      config <- IO.fromEither(configOrError)
      given Logger[IO] <- Slf4jLogger.create[IO]
      start <- IO.realTimeInstant
      completionTime = start.plusSeconds(config.totalDuration.toSeconds)
      _ <- Logger[IO].info(s"starting ESP32-CAM client. Job completion time: $completionTime")
      _ <- EmberClientBuilder.default[IO].build.use { client =>
        val imageServerClient = ImageServerClient[IO](client, config.imageServerUri, config.imageDir)
        imageServerClient.setResolution(Resolution.HD) *>
          fs2.Stream.fixedRate[IO](config.intervals)
            .evalMap(_ => imageServerClient.getImage)
            .takeWhile(_ => Instant.now().isBefore(completionTime))
            .compile
            .drain
      }
      _ <- Logger[IO].info("Job completed, stopping now...")
    yield ()

