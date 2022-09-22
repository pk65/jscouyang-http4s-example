package com.your.domain.jscouyanghttp4s

import cats.syntax.all._

package object route {
  val all = joke.CRUD <+> joke.random <+> config.get
}
