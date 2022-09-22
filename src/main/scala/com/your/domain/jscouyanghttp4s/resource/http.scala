package com.your.domain.jscouyanghttp4s
package resource

import cats.effect._
import org.http4s._
import org.http4s.client.Client
import org.http4s.finagle.Finagle
import com.twitter.finagle.Http
import com.twitter.finagle.stats.NullStatsReceiver
import org.http4s.Uri.Scheme
import zipkin2.finagle.http.HttpZipkinTracer

trait HasClient {
  val jokeClient: Client[IO]
}

object http {

  def mk(uri: Uri, tracerHost: String)(implicit ctx: ContextShift[IO]): Resource[IO, Client[IO]] = {
    def tracer = {
      HttpZipkinTracer.create(
        HttpZipkinTracer.Config
          .builder()
          .host(tracerHost)
          .build(),
        new NullStatsReceiver(),
      )
    }
    (uri.scheme, uri.host, uri.port) match {
      case (Some(Scheme.https), Some(host), None) =>
        Finagle.mkClient[IO](
          Http.client.withHttp2
            .withTls(host.value)
            .withHttpStats
            .withStatsReceiver(PrometheusExporter.metricStatsReceiver)
            .withTracer(tracer)
            .newService(s"$host:443")
        )
      case (Some(Scheme.https), Some(host), Some(port)) =>
        Finagle.mkClient[IO](
          Http.client.withHttp2
            .withTls(host.value)
            .withHttpStats
            .withStatsReceiver(PrometheusExporter.metricStatsReceiver)
            .withTracer(tracer)
            .newService(s"$host:$port")
        )
      case (_, Some(host), Some(port)) =>
        Finagle.mkClient[IO](
          Http.client.withHttpStats.withHttp2
            .withStatsReceiver(PrometheusExporter.metricStatsReceiver)
            .withTracer(tracer)
            .newService(s"$host:$port")
        )
      case _ =>
        Resource.eval(IO.raiseError(new Exception(s"cannot initialize HttpClient for $uri")))
    }
  }
}
