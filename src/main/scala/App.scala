import org.http4s.ember.client.EmberClientBuilder
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.either.catsSyntaxEither
import scala.concurrent.duration.*

import java.time.Instant

object App extends IOApp.Simple:
  override def run: IO[Unit] =
    val configOrError = Config
      .fromEnv(sys.env)
      .toEither
      .leftMap(errors => new RuntimeException(s"Failed to read configs:${errors.toList.mkString("\n", "\n", "\n")}"))
    for
      config <- IO.fromEither(configOrError)
      _ <- IO.println("starting ESP32-CAM client")
      start <- IO.realTimeInstant
      _ <- EmberClientBuilder.default[IO].build.use { client =>
        val imageServerClient = ImageServerClient[IO](client, config.imageServerUri, config.imageDir)
        fs2.Stream.fixedRate[IO](config.intervals)
          .evalMap(_ => imageServerClient.getImage)
          .takeWhile(_ => Instant.now().isBefore(start.plusSeconds(config.totalDuration.toSeconds)))
          .compile
          .drain
      }
    yield ()

