package db.migration

import com.your.domain.jscouyanghttp4s.db.DoobieMigration
import doobie.implicits._

class V1_1__AddUpdatedColumn extends DoobieMigration {
  override def migrate =
    sql"""ALTER TABLE `joke`
    CHANGE COLUMN `created` `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN `updated_at` TIMESTAMP(6) NULL AFTER `created_at`""".update.run
}
