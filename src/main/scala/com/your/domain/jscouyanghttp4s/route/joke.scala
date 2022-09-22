package com.your.domain.jscouyanghttp4s
package route

import cats.data._
import cats.effect._
import cats.implicits._
import com.twitter.logging.Logger
import com.your.domain.jscouyanghttp4s.AppDsl._
import com.your.domain.jscouyanghttp4s.resource._
import com.your.domain.jscouyanghttp4s.resource.database.context._
import com.your.domain.jscouyanghttp4s.resource.logger._
import io.circe.generic.auto._
import io.circe.literal._
import org.http4s.circe.CirceEntityCodec._

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext

object joke {
  implicit val log: Logger = Logger.get()
  case class DadJoke(id: String, joke: String)
  val dadJokeApp: Kleisli[IO, AppResource, DadJoke] = log.infoF("getting dad joke...") *>
    Kleisli.ask[IO, HasClient].flatMapF(_.jokeClient.expect[DadJoke]("/"))

  val random: AppRoute = AppRoute {
    case GET -> Root / "random-joke" =>
      log.infoF("generating random joke") *>
        dadJokeApp.flatMap(Ok(_))
  }

  object Dao {
    case class Joke(id: UUID, text: String, created_at: Instant, updated_at: Instant)
  }
  object Repr {
    case class View(id: UUID, text: String, created_at: ZonedDateTime, updated_at: ZonedDateTime)
    case class Create(text: String)
    object View {
      def from(db: Dao.Joke): View = View(db.id, db.text, getZoneTime(db.created_at), getZoneTime(db.updated_at))

      val zone: ZoneId = ZoneId.systemDefault()
      val utc = ZoneId.of("UTC")
      val epoch: Instant = Instant.parse("0000-01-01T00:00:00Z")

      private def getZoneTime(time: Instant) = {
        if (time.isBefore(epoch)) epoch.atZone(utc) else time.atZone(zone)
      }
    }
  }
  val CRUD: AppRoute = AppRoute {
    case req @ POST -> Root / "joke" =>
      val id = UUID.randomUUID()
      for {
        has <- Kleisli.ask[IO, HasDatabase]
        joke <- Kleisli.liftF(req.as[Repr.Create])
        _ <- has.transact(run(quote {
          query[Dao.Joke]
            .insert(_.id -> lift(id), _.text -> lift(joke.text))
        }))
        _ <- log.infoF(s"created joke with id $id")
        resp <- Created(json"""{ "id": $id }""")
      } yield resp

    case GET -> Root / "joke" =>
      Kleisli
        .ask[IO, HasDatabase]
        .flatMap(
          db =>
            Ok(
              db.transact(stream(quote {
                query[Dao.Joke]
              })).map(Repr.View.from)
            )
        )

    case GET -> Root / "joke" / UUIDVar(id) =>
      for {
        has <- Kleisli.ask[IO, HasDatabase with HasToggle]
        joke <- log.infoF(s"getting joke $id") *> Kleisli.liftF(
          IO.shift(IO.contextShift(ExecutionContext.global))
        ) *> has.transact(run(quote {
          query[Dao.Joke].filter(_.id == lift(id)).take(1)
        }))
        dadJoke =
          if (has.toggleOn("com.your.domain.jscouyanghttp4s.useDadJoke"))
            log.infoF(s"cannot find joke $id") *> dadJokeApp.flatMap(NotFound(_))
          else
            NotFound(json"""{ "id": $id, "error": "Not found"}""")
        resp <- joke match {
          case a :: Nil => Ok(Repr.View.from(a))
          case _        => dadJoke
        }
      } yield resp

    case req @ PUT -> Root / "joke" / UUIDVar(id) =>
      for {
        db <- Kleisli.ask[IO, HasDatabase]
        joke <- Kleisli.liftF(req.as[Repr.Create])
        ret <- db.transact(run(quote {
          query[Dao.Joke].filter(_.id == lift(id)).update(_.text -> lift(joke.text), _.updated_at -> lift(Instant.now()))
        }))
        resp <- Ok(json"""{ "id": $id, "updated": $ret }""")
      } yield resp

    case DELETE -> Root / "joke" / UUIDVar(id) =>
      Kleisli
        .ask[IO, HasDatabase]
        .flatMap(_.transact(run(quote {
          query[Dao.Joke].filter(_.id == lift(id)).delete
        }))) >> NoContent()
  }
}
