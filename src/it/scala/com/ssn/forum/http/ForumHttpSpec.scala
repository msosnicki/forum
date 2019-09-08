package com.ssn.forum.http

import java.util.UUID

import cats.effect.{ExitCode, Fiber, IO, Timer}
import cats.implicits._
import com.softwaremill.sttp.circe._
import com.ssn.forum.Main
import com.ssn.forum.config.{AppConfig, DbConfig}
import com.ssn.forum.di.ProdModule
import com.ssn.forum.test.IOFlatSpec
import org.scalatest.{BeforeAndAfterAll, DiagrammedAssertions}

import scala.concurrent.ExecutionContext.global

class ForumHttpSpec extends IOFlatSpec with DiagrammedAssertions with BeforeAndAfterAll {
  import com.ssn.forum.http.TestRequests._

  behavior of "Forum Http endpoints"

  implicit val timer: Timer[IO] = IO.timer(global)

  val MaxPosts = 10

  val config = AppConfig(DbConfig("jdbc:postgresql://localhost:5432/forum_app", "my_user", "my_pass"), MaxPosts)

  //Alternatively, this could be in docker.
  val server: Fiber[IO, ExitCode] = {
    Main.run0[IO](new ProdModule[IO](config)).use(_ => IO.never).as(ExitCode.Success).start.unsafeRunSync()
  }

  override def afterAll(): Unit =
    server.cancel.unsafeRunSync()

  behavior of "POST /topic/"

  it should "create successfully" in {
    for {
      resp <- createTopic()
    } yield {
      assert(resp.code == 201)
      val body = resp.body
      assert(body.topicId > 0)
      assert(body.postId > 0)
    }
  }

  it should "fail gracefully if the topic name is too long" in {
    for {
      resp <- createTopicRaw(createTopicRequest.copy(subject = stringOfLength(500)))
    } yield {
      assert(resp.code == 400)
    }
  }

  it should "not create if the email is invalid" in {
    for {
      resp <- createTopicRaw(createTopicRequest.copy(email = invalidEmail))
    } yield {
      assert(resp.code == 400)
    }
  }

  behavior of "POST /topic/${topicId}/post"

  it should "create successfully" in {
    for {
      t    <- createTopic()
      resp <- createPost(t.body.topicId)
    } yield {
      assert(resp.code == 201)
      val body = resp.body
      assert(body.id > 0)
      assert(resp.getAuthHeader.isDefined)
    }
  }

  it should "not create if the email is invalid" in {
    for {
      t    <- createTopic()
      resp <- createPostRaw(t.body.topicId, createPostRequest.copy(email = invalidEmail))
    } yield {
      assert(resp.code == 400)
    }
  }

  it should "fail gracefully if the nickname is too long" in {
    for {
      t    <- createTopic()
      resp <- createPostRaw(t.body.topicId, createPostRequest.copy(nickname = stringOfLength(50)))
    } yield {
      assert(resp.code == 400)
    }
  }

  behavior of "GET /topic/?offset=${offset}&limit=${limit}"

  it should "keep an order of newly created topics" in {
    for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t2.body.topicId)
      assert(tl2.id == t1.body.topicId)
    }
  }

  it should "move topic to the top when the new post is added" in {
    for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      _      <- createPost(t1.body.topicId, createPostRequest.copy(text = "new post in old topic"))
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t1.body.topicId)
      assert(tl2.id == t2.body.topicId)
    }
  }

  it should "keep original order if post is first added and then deleted" in {
    for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      p      <- createPost(t1.body.topicId, createPostRequest.copy(text = "new post in old topic"))
      _      <- deletePost(t1.body.topicId, p.body.id, p.getAuthHeader.get)
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t2.body.topicId)
      assert(tl2.id == t1.body.topicId)
    }
  }

  it should "return an error if the limit is too big" in {
    (for {
      resp <- listTopicsRaw(0, 100)
    } yield {
      assert(resp.code == 400)
    }).unsafeToFuture()
  }

  behavior of "GET /topic/${topicId}/post/id?before=${before}&after=${after}"

  it should "return just the given row when no before and limit is specified" in {
    for {
      t <- createTopic()
      p <- createPost(t.body.topicId)
      l <- listPosts(t.body.topicId, p.body.id)
    } yield {
      assert(l.body.posts.size == 1)
      assert(l.body.posts.head.id == p.body.id)
    }
  }

  it should "return both before and after if below limit" in {
    for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), Some(2), Some(2))
    } yield {
      assert(l.body.posts.map(_.id) == ids.reverse)
    }
  }

  it should "return just before if after is not specified" in {
    for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), Some(2), None)
    } yield {
      assert(l.body.posts.map(_.id) == ids.take(3).reverse)
    }
  }

  it should "return just after if below is not specified" in {
    for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), None, Some(2))
    } yield {
      assert(l.body.posts.map(_.id) == ids.drop(2).reverse)
    }
  }

  it should "paginate around post if limit is not exceeded" in {
    for {
      t <- createTopic()
      p <- List.fill(20)(createPost(t.body.topicId)).sequence
      ids        = p.map(_.body.id)
      idsOrdered = ids.reverse
      id         = ids(10)
      l <- listPosts(t.body.topicId, id, Some(5), Some(4))
    } yield {
      val idObj = l.body.posts.find(_.id == id)
      assert(idObj.isDefined)
      assert(l.body.posts.size == MaxPosts)
      val after  = l.body.posts.filter(_.createdAt.isAfter(idObj.get.createdAt))
      val before = l.body.posts.filter(_.createdAt.isBefore(idObj.get.createdAt))
      assert(after.size == 4)
      assert(after.map(_.id) == idsOrdered.slice(5, 9))
      assert(before.size == 5)
      assert(before.map(_.id) == idsOrdered.slice(10, 15))
    }
  }

  /**
    * So after rounding up we have:
    * max = 10
    * (20/25) * (10 - 1) ~= 7.2 ~= 7
    * 9 - 7 = 2
    * So here proportionally after should have 7 elements and before 2.
    * The problem is that there are only 4 elements after in db. So the remaining 3 should be added up by the before.
    * So final version would be after == 4 & before == 5
    */
  it should "adjust older records if there is not enough newer" in {
    for {
      t <- createTopic()
      p <- List.fill(20)(createPost(t.body.topicId)).sequence
      ids        = p.map(_.body.id)
      idsOrdered = ids.reverse
      id         = ids(15)
      l <- listPosts(t.body.topicId, id, Some(5), Some(20))
    } yield {
      val idObj = l.body.posts.find(_.id == id)
      assert(idObj.isDefined)
      assert(l.body.posts.size == MaxPosts)
      val after  = l.body.posts.filter(_.createdAt.isAfter(idObj.get.createdAt))
      val before = l.body.posts.filter(_.createdAt.isBefore(idObj.get.createdAt))
      assert(after.size == 4)
      assert(after.map(_.id) == idsOrdered.take(4))
      assert(before.size == 5)
      assert(before.map(_.id) == idsOrdered.slice(5, 10))
    }
  }

  /**
    * So here proportionally after should have 2 elements and before 7.
    * There are only 4 rows before so so the remaining 3 are added to 2.
    * So final version would be after == 5 & before == 4
    */
  it should "adjust newer records if there is not enough older" in {
    for {
      t <- createTopic()
      _ <- deletePost(t.body.topicId, t.body.postId, t.getAuthHeader.get)
      p <- List.fill(20)(createPost(t.body.topicId)).sequence
      ids        = p.map(_.body.id)
      idsOrdered = ids.reverse
      id         = ids(4)
      l <- listPosts(t.body.topicId, id, Some(20), Some(5))
    } yield {
      val idObj = l.body.posts.find(_.id == id)
      assert(idObj.isDefined)
      assert(l.body.posts.size == MaxPosts)
      val after  = l.body.posts.filter(_.createdAt.isAfter(idObj.get.createdAt))
      val before = l.body.posts.filter(_.createdAt.isBefore(idObj.get.createdAt))
      assert(before.size == 4)
      assert(after.size == 5)
      assert(after.map(_.id) == idsOrdered.slice(10, 15))
      assert(before.map(_.id) == idsOrdered.takeRight(4))
    }
  }

  it should "return error if numbers are negative" in {
    for {
      l <- listPostsRaw(Long.MaxValue, Long.MaxValue, Some(-1), Some(-2))
    } yield {
      assert(l.code == 400)
    }
  }

  behavior of "PUT /topic/${topicId}/post/${postId}"

  it should "edit an existent post" in {
    for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- editPost(t.body.topicId, p.body.id, p.getAuthHeader.get, "Updated post")
    } yield {
      assert(resp.code == 200)
    }
  }

  it should "not edit an existent post if there is a topicId mismatch" in {
    for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- editPostRaw(t.body.topicId + 1, p.body.id, p.getAuthHeader.get, "Updated post")
    } yield {
      assert(resp.code == 304)
    }
  }

  it should "return valid codes if post doesn't exist" in {
    for {
      resp <- editPostRaw(Long.MaxValue, Long.MaxValue, randomToken, "Updated post")
    } yield {
      assert(resp.code == 403)
    }
  }

  behavior of "DELETE /topic/${topicId}/post/${postId}"

  it should "delete an existent post" in {
    for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePost(t.body.topicId, p.body.id, p.getAuthHeader.get)
    } yield {
      assert(resp.code == 200)
    }
  }

  it should "not delete an existent post if there is a topicId mismatch" in {
    for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePostRaw(t.body.topicId + 1, p.body.id, p.getAuthHeader.get)
    } yield {
      assert(resp.code == 304)
    }
  }

  it should "return good code if the token is invalid" in {
    for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePostRaw(t.body.topicId, p.body.id, randomToken)
    } yield {
      assert(resp.code == 403)
    }
  }

  it should "return valid codes if post doesn't exist" in {
    for {
      resp <- deletePostRaw(Long.MaxValue, Long.MaxValue, randomToken)
    } yield {
      assert(resp.code == 403)
    }
  }

  behavior of "GET /unknown/endpoint"

  it should "return 404" in {
    for {
      resp <- get("/unknown/endpoint")
    } yield {
      assert(resp.code == 404)
    }
  }

  private val randomToken  = UUID.randomUUID()
  private val invalidEmail = "invalid email"

  private def stringOfLength(l: Int) =
    List.fill(l)("a").mkString

}
