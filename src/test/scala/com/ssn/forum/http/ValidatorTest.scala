package com.ssn.forum.http

import com.ssn.forum.http.Validator.BeforeAndAfter
import org.scalatest.{FlatSpec, Inside}

class ValidatorTest extends FlatSpec with Inside {

  //TODO: add more unit tests in whole project ;)

  behavior of "limiting before and after"

  it should "return values nicely if limit is not exceeded" in {
    val value = Validator.validateBeforeAndAfter(200)(Some(100), Some(50))
    inside(value) {
      case Right(BeforeAndAfter(before, after, cropped)) =>
        assert(before == 100)
        assert(after == 50)
        assert(!cropped)
    }
  }

  it should "round values nicely if limit is exceeded" in {
    val value = Validator.validateBeforeAndAfter(50)(Some(100), Some(50))
    inside(value) {
      case Right(BeforeAndAfter(before, after, cropped)) =>
        assert(before == 33) //32.667
        assert(after == 16)  //16.333
        assert(cropped)
    }
  }

  it should "return zeros if nothing is given" in {
    val value = Validator.validateBeforeAndAfter(50)(None, None)
    inside(value) {
      case Right(BeforeAndAfter(before, after, cropped)) =>
        assert(before == 0)
        assert(after == 0)
        assert(!cropped)
    }
  }

  it should "return error if negative before given" in {
    val value = Validator.validateBeforeAndAfter(50)(Some(-1), None)
    inside(value) {
      case Left(_) => succeed
    }
  }

  it should "return error if negative after given" in {
    val value = Validator.validateBeforeAndAfter(50)(None, Some(-1))
    inside(value) {
      case Left(_) => succeed
    }
  }

}
