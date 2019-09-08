package com.ssn.forum.test

import cats.effect.IO
import org.scalactic.source
import org.scalatest.{compatible, AsyncFlatSpec}

abstract class IOFlatSpec extends AsyncFlatSpec {
  implicit def toItVerbStringOps(v: ItVerbString): ItVerbStringOps             = new ItVerbStringOps(v)
  implicit def toIgnoreVerbStringOps(v: IgnoreVerbString): IgnoreVerbStringOps = new IgnoreVerbStringOps(v)
  final class ItVerbStringOps(v: ItVerbString) {
    def in(testFun: => IO[compatible.Assertion])(implicit pos: source.Position): Unit =
      v.in(testFun.unsafeToFuture())
  }

  final class IgnoreVerbStringOps(v: IgnoreVerbString) {
    def in(testFun: => IO[compatible.Assertion])(implicit pos: source.Position): Unit =
      v.in(testFun.unsafeToFuture())
  }
}
