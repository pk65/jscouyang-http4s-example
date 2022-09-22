package com.your.domain.jscouyanghttp4s
package route
import munit._
import org.scalacheck.Prop._

import java.util.UUID
import org.http4s.dsl.io._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.mockito.MockitoSugar._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalacheck.ScalacheckShapeless._
import io.circe.literal._
import io.circe._
import cats.effect._
import org.http4s.client.Client
import com.twitter.finagle.toggle._
import org.http4s.HttpRoutes
import org.http4s.HttpApp

class JokeSpec extends FunSuite with ScalaCheckSuite with SpecHelper {
  override val scalaCheckInitialSeed = "lYrJmpifMv1Xx0ZR0OpISKCa-oE0pPxVF6TDBVMzEpE="
  private val useDadJokeToggleName = "com.your.domain.jscouyanghttp4s.useDadJoke"
  private val randomId = UUID.randomUUID()

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(10)
      .withMaxDiscardRatio(3)
  def update(id: String, req: joke.Repr.Create)(implicit router: HttpApp[IO]) =
    router(PUT(req.asJson, uri"/joke" / id).unsafeRunSync())

  def query(id: String)(implicit router: HttpApp[IO]) = router(GET(uri"/joke" / id).unsafeRunSync())
  private def create(req: joke.Repr.Create)(implicit router: HttpApp[IO]) =
    for {
      reqCreate <- POST(req.asJson, uri"/joke")
      created <- router(reqCreate)
      id <- created.as[Json].map(_.hcursor.get[UUID]("id"))
    } yield id.getOrElse(UUID.randomUUID()).toString

  private def delete(id: String)(implicit router: HttpApp[IO]) =
    router(DELETE(uri"/joke" / id).unsafeRunSync())
      .map(_ => assertEquals(query(id).unsafeRunSync().status, NotFound))

  def createAndDelete(req: joke.Repr.Create)(implicit router: HttpApp[IO]) =
    Resource.make[IO, String](create(req))(delete)

  property("CRUD") {
    implicit val appRes = new TestAppResource
    forAll { (requestBody: joke.Repr.Create, updateBody: joke.Repr.Create) =>
      when(appRes.toggleMap.apply(useDadJokeToggleName))
        .thenReturn(Toggle.off(useDadJokeToggleName))
      createAndDelete(requestBody)
        .use { id =>
          assertEquals(query(id).flatMap(_.as[joke.Repr.View]).unsafeRunSync().text, requestBody.text)
          update(id, updateBody)
            .map(_ => assertEquals(query(id).flatMap(_.as[joke.Repr.View]).unsafeRunSync().text, updateBody.text))
        }
        .unsafeRunSync()
    }
  }

  test("get dad joke when toggle on") {
    val dadJoke = json"""{
        "id": "R7UfaahVfFd",
        "joke": "My dog used to chase people on a bike a lot. It got so bad I had to take his bike away."
      }"""
    implicit val appRes = new TestAppResource {
      override val jokeClient = Client.fromHttpApp(
        HttpRoutes
          .of[IO] {
            case GET -> _ => Ok(dadJoke)
          }
          .orNotFound
      )
    }

    when(appRes.toggleMap.apply(useDadJokeToggleName))
      .thenReturn(Toggle.on(useDadJokeToggleName))

    val resp1 = router.run(GET(uri"/joke" / randomId.toString).unsafeRunSync()).flatMap(_.as[Json])
    assertEquals(resp1.unsafeRunSync(), dadJoke)

    when(appRes.toggleMap.apply(useDadJokeToggleName))
      .thenReturn(Toggle.off(useDadJokeToggleName))

    val resp2 = router.run(GET(uri"/joke" / randomId.toString).unsafeRunSync())
    assertEquals(resp2.unsafeRunSync().status, NotFound)
  }
}
