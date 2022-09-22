package com.your.domain.jscouyanghttp4s

import cats.data.OptionT
import cats.effect._
import com.twitter.finagle.Http
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import org.http4s.HttpRoutes
import org.http4s.finagle.Finagle
import org.http4s.implicits._
import zipkin2.finagle.http.HttpZipkinTracer
import scala.concurrent.ExecutionContext

object Main extends TwitterServer with PrometheusExporter {
  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val port = flag("port", ":8080", "Service Port Number")

  def main() =
    resource.mk
      .use { (deps: Resource[IO, AppResource]) =>
        val service: HttpRoutes[IO] = route.all.flatMapF(resp => OptionT.liftF(deps.use(r => resp.run(r))))
        val tracerHost = deps.use(r => IO.pure(r.config.app.tracerHost)).unsafeRunSync()
        logger.info(s"Tracer Host configured: ${tracerHost}")
        val server = Http.server
          .withTracer(HttpZipkinTracer.create(
            HttpZipkinTracer.Config
              .builder()
              .host(tracerHost)
              .build(), new NullStatsReceiver()))
          .withHttp2
          .withHttpStats
          .withStatsReceiver(PrometheusExporter.metricStatsReceiver)
          .withLabel("jscouyang-http4s-X")
          .serve(port(), Finagle.mkService[IO](service.orNotFound))
        logger.info(s"Server Started on ${port()}")
        onExit {
          server.close()
          ()
        }
        IO(Await.ready(server))
      }
      .unsafeRunSync()
}
