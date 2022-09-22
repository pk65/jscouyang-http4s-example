package com.your.domain.jscouyanghttp4s
package route

import cats.data._
import cats.effect._
import cats.implicits._
import com.twitter.logging.Logger
import com.your.domain.jscouyanghttp4s.AppDsl._
import com.your.domain.jscouyanghttp4s.resource.logger._

object config {
  implicit val log = Logger.get()
  val get = AppRoute {
    case GET -> Root / "diagnostic" / "config" =>
      log.infoF("getting config sha") *>
        Kleisli.ask[IO, HasConfig].flatMap(h => Ok(h.config.sha))
  }

}
