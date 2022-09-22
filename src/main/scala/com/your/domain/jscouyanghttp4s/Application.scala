package com.your.domain.jscouyanghttp4s

import org.http4s.Uri

case class Application(jokeService: Uri, tracerHost: String, database: DataBaseConfig)
case class Port(number: Int) extends AnyVal
case class DataBaseConfig(driver: String, host: String, port: Port, name: String, user: String, pass: String) {
  def jdbc = s"jdbc:${driver}://${host}:${port.number}/${name}"
  def driverClass = driver match {
    case "postgresql" => "org.postgresql.Driver"
    case "h2" => "org.h2.Driver"
    case _ => "com.mysql.cj.jdbc.Driver"
  }
}
