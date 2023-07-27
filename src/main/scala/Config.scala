import org.http4s.Uri
import cats.Applicative
import cats.data.NonEmptyList
import cats.data.NonEmptyList.catsDataSemigroupForNonEmptyList
import cats.data.Validated
import cats.data.Validated.catsDataApplicativeErrorForValidated
import cats.data.ValidatedNel
import cats.syntax.either.catsSyntaxEither
import cats.syntax.either.catsSyntaxEitherId
import cats.syntax.validated.catsSyntaxValidatedId
import cats.syntax.option.catsSyntaxOption
import cats.syntax.option.catsSyntaxOptionId

import scala.concurrent.duration.*
import scala.util.Try

case class Config(imageServerUri: Uri, imageDir: String, totalDuration: FiniteDuration, intervals: FiniteDuration)

object Config:
  type ValidationErrorOr[T] = ValidatedNel[String, T]

  def fromEnv(envVars: Map[String, String]): ValidatedNel[String, Config] =
    def getOrInvalid(name: String): ValidatedNel[String, String] =
      envVars.get(name).toValidNel(s"Variable [$name] not found")

    Applicative[ValidatedNel[String, _]].map4(
      getOrInvalid("ESP32_URI")
        .andThen(Uri.fromString(_).leftMap(e => s"Failed to parse ESP32 server Uri: ${e.message}").toValidatedNel),
      getOrInvalid("ESP32_IMG_DIR"),
      getOrInvalid("ESP32_RECORDING_MINUTES")
        .andThen(minutes =>
          minutes.toLongOption.filter(_ > 0).map(_.minutes)
            .toValidNel(s"Failed to parse Recording minutes of [$minutes]")
        ),
      getOrInvalid("ESP32_CAPTURE_INTERVALS_SECONDS")
        .andThen(seconds =>
          seconds.toLongOption.filter(_ > 0).map(_.seconds)
            .toValidNel(s"Failed to parse Capturing intervals seconds of [$seconds]")
        )
    )(Config.apply)

