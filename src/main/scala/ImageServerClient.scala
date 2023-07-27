import cats.MonadThrow
import cats.Monad
import cats.FlatMap
import cats.effect.{Async, Temporal}
import cats.effect.std.Console
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import cats.syntax.apply.catsSyntaxApply
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import fs2.io.file.Path
import org.http4s.client.Client
import org.http4s.Uri
import org.http4s.EntityDecoder
import org.http4s.EntityDecoder.binFile

import java.time.Instant

class ImageServerClient[F[_] : Async : MonadThrow : Console](client: Client[F], serverUri: Uri, imageDir: String):
  def getImage: F[Unit] =
    Temporal[F].delay(Instant.now()).flatMap { timeBefore =>
      client.get(serverUri) { response =>
        val now = Instant.now()

        given EntityDecoder[F, Path] = binFile(Path(s"$imageDir/image_${now.getEpochSecond}.jpg"))

        response.as[Path] *> Console[F].println(s"image requested at $timeBefore and received at $now and processed at ${Instant.now()}")
      }.handleErrorWith { e =>
        Console[F].println("Failed to fetch data from image server, skipping") *> Temporal[F].delay(e.printStackTrace())
      }

    }
