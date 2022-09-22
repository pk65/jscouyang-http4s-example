package db.migration

import com.your.domain.jscouyanghttp4s.db.DoobieMigration
import doobie.implicits._

class V1_0__CreateJokeTable extends DoobieMigration {
  override def migrate =
    sql"""CREATE TABLE `joke` (
      `id` BINARY(16) NOT NULL DEFAULT 0,
      `text` tinytext COLLATE utf8mb4_polish_ci NOT NULL,
      `created` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci""".update.run
}
