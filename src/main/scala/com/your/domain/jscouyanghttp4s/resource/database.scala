package com.your.domain.jscouyanghttp4s
package resource

import cats.effect._
import doobie._
import doobie.hikari._
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}
import fs2._

import java.sql.{Timestamp, Types}
import java.time.Instant
import java.util.UUID

trait HasDatabase {

  val database: Transactor[IO]

  def transact[A](c: ConnectionIO[A]): App[A] =
    NT.IOtoApp(c.transact(database))

  def transact[A](c: Stream[ConnectionIO, A]): Stream[IO, A] =
    c.transact(database)
}

class MyDatabaseContext extends DoobieContext.MySQL(SnakeCase)

object database {
  val context = new MyDatabaseContext {
    implicit val InstantDecoder: Decoder[Instant] =
      decoder((index, row) => getTimestamp(row.getTimestamp(index)))

    implicit val InstantEncoder: Encoder[Instant] =
      encoder(Types.TIMESTAMP, (index, value, row) => row.setTimestamp(index, Timestamp.from(value)))
    implicit val InstantOptEncoder: Encoder[Option[Instant]] =
      encoder(Types.TIMESTAMP, (index, value, row) => value match {
        case Some(time) => row.setTimestamp(index, Timestamp.from(time))
        case None => row.setNull(index, Types.TIMESTAMP)
      })

    implicit val UUIDEncoder: Encoder[UUID] =
      encoder(Types.BINARY, (index, uuid, row) => row.setBytes(index, BigInt(uuid.getMostSignificantBits).toByteArray ++ BigInt(uuid.getLeastSignificantBits).toByteArray))
    implicit val UUIDDecoder: Decoder[UUID] =
      decoder((index, row) => nameUUIDFromBytes(row.getBytes(index)))

    private def nameUUIDFromBytes(bytes: Array[Byte]) = {
      val hex1 = String.format("%016x", BigInt(bytes.slice(0,8)).longValue)
      val hex2 = String.format("%016x", BigInt(bytes.slice(8,16)).longValue)
      UUID.fromString(String.format("%s-%s-%s-%s-%s", hex1.slice(0, 8), hex1.slice(8, 12), hex1.slice(12, 16), hex2.slice(0, 4), hex2.slice(4, 16)))
    }

    private def getTimestamp(timestamp: Timestamp) = {
      if (timestamp == null)  Instant.MIN else timestamp.toInstant
    }
  }



  def transactor(db: DataBaseConfig)(implicit ctx: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      be <- Blocker[IO]
      xa <- HikariTransactor.newHikariTransactor[IO]("com.mysql.cj.jdbc.Driver", db.jdbc, db.user, db.pass, ce, be)
    } yield xa
}
