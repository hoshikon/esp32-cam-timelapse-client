import cats.{FlatMap, Monad, MonadThrow}
import cats.effect.{Async, Temporal}
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import cats.syntax.apply.catsSyntaxApply
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import fs2.io.file.Path
import org.http4s.EntityDecoder.binFile
import org.http4s.{EntityDecoder, Uri}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

import java.time.Instant
import java.time.temporal.ChronoUnit

class ImageServerClient[F[_] : Async : MonadThrow : Logger](client: Client[F], serverUri: Uri, imageDir: String):
  def getImage: F[Unit] =
    Temporal[F].delay(Instant.now()).flatMap { timeBefore =>
      client.get(serverUri) { response =>
        val now = Instant.now()

        given EntityDecoder[F, Path] = binFile(Path(s"$imageDir/image_${now.getEpochSecond}.jpg"))

        response.as[Path] *> Logger[F].info(s"[requested: ${timeBefore.truncatedTo(ChronoUnit.SECONDS)}] [received: ${now.truncatedTo(ChronoUnit.SECONDS)}] [processed: ${Instant.now().truncatedTo(ChronoUnit.SECONDS)}]")
      }.handleErrorWith { e =>
        Logger[F].error(e)("Failed to fetch data from image server, skipping")
      }

    }
