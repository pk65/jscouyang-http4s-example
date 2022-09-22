package com.your.domain.jscouyanghttp4s
import cats.effect.IO
import org.http4s.Uri
import pureconfig.ConfigConvert.{catchReadError, viaNonEmptyString}

import pureconfig._
import pureconfig.generic.auto._


case class Config(app: Application, sha: String)

object Config {
  implicit val uriConvert = viaNonEmptyString[Uri](
    catchReadError(Uri.unsafeFromString(_)), _.toString())
  def all: IO[Config] = {
    ConfigSource.default.load[Application] match {
      case Left(configReaderFailures) =>
        throw new Exception(configReaderFailures.toList.mkString("\n"))
      case Right(app) => IO.pure(Config(app, ""))
    }
  }
}

trait HasConfig {
  val config: Config
}
