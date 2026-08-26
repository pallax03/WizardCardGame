package io.github.pallax03.wizard.util

import scala.util.Random

/** Utility object for generating unique identifiers. */
object Id:
  def apply(idBase: Int = 36): String =
    BigInt.long2bigInt(Random.nextLong(Long.MaxValue)).toString(idBase)
