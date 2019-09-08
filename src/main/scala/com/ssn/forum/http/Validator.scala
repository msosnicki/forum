package com.ssn.forum.http

import com.ssn.forum.exceptions.{BeforeOrAfterNegative, PaginateLimitException}

object Validator {
  def validateLimit(max: Int)(value: Int): Either[PaginateLimitException, Int] =
    if (value > 0 && value <= max) Right(value)
    else Left(PaginateLimitException(value, max))

  def validateBeforeAndAfter(
      max: Int
  )(before: Option[Int], after: Option[Int]): Either[BeforeOrAfterNegative.type, BeforeAndAfter] = {
    val (b, a) = (before.getOrElse(0), after.getOrElse(0))
    if (a < 0 || b < 0) Left(BeforeOrAfterNegative)
    else if (b + a < max) Right(BeforeAndAfter(before.getOrElse(0), after.getOrElse(0), cropped = false))
    else {
      val beforeNo = Math.round(b.toFloat / (a + b) * (max - 1))
      Right(BeforeAndAfter(beforeNo, max - 1 - beforeNo, cropped = true))
    }
  }

  case class BeforeAndAfter(before: Int, after: Int, cropped: Boolean)
}
