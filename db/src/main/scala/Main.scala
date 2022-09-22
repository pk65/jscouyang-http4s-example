package com.your.domain.jscouyanghttp4s.db

import cats.effect._
import com.typesafe.config.{Config, ConfigFactory}
import org.flywaydb.core.Flyway


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val c: Config = ConfigFactory.load().getConfig("database")
    val flyway = Flyway.configure.dataSource(
      s"jdbc:${c.getString("driver")}://${c.getString("host")}:${c.getString("port")}/${c.getString("name")}",
      c.getString("user"),
      c.getString("pass"),
    )
    args match {
      case List("migrate") =>
        IO(flyway.load.migrate()).as(ExitCode.Success)
      case List("clean") =>
        IO(flyway.load.clean()).as(ExitCode.Success)
      case a =>
        IO(System.err.println(s"""|Unknown args $a
             |Usage:
             |  sbt "db/run migrate|clean"                 migrate local
             |  env DB_HOST=<host> DB_PORT=<port> \\
             |      DB_NAME=<database> DB_USER=<user> \\
             |      DB_PASS=<pass> \\
             |      sbt "db/run migrate|clean"             migrate a specified db""".stripMargin)).as(ExitCode(2))
    }
  }
}
