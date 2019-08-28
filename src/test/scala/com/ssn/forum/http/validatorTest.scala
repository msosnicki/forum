package com.ssn.forum.http

import org.scalatest.{FlatSpec, Inside}

class validatorTest extends FlatSpec with Inside {

  behavior of "limiting before and after"

  it should "round values nicely if limit is exceeded" in {
    val value = validator.validateBeforeAndAfter(50)(Some(100), Some(50))
    inside(value) {
      case Right((before, after)) =>
        assert(before == 33) //32.667
        assert(after == 16)  //16.333
    }
  }

  it should "return zeros if nothing is given" in {
    val value = validator.validateBeforeAndAfter(50)(None, None)
    inside(value) {
      case Right((before, after)) =>
        assert(before == 0)
        assert(after == 0)
    }
  }

  it should "return error if negative before given" in {
    val value = validator.validateBeforeAndAfter(50)(Some(-1), None)
    inside(value) {
      case Left(_) => succeed
    }
  }

  it should "return error if negative after given" in {
    val value = validator.validateBeforeAndAfter(50)(None, Some(-1))
    inside(value) {
      case Left(_) => succeed
    }
  }

}
