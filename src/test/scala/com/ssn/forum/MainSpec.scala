package com.ssn.forum

import java.time.Instant
import java.util.UUID

import cats.implicits._
import cats.effect.{ExitCode, Fiber, IO, Timer}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, DiagrammedAssertions}
import com.softwaremill.sttp.circe._

import scala.concurrent.ExecutionContext.global

//TODO: move to integration tests
class MainSpec extends AsyncFlatSpec with DiagrammedAssertions with BeforeAndAfterAll {
  import com.ssn.forum.http.TestRequests._

  behavior of "Forum app"

  implicit val timer: Timer[IO] = IO.timer(global)

  val server: Fiber[IO, ExitCode] =
    Main.run0[IO].use(_ => IO.never).as(ExitCode.Success).start.unsafeRunSync()

  override def afterAll(): Unit =
    server.cancel.unsafeRunSync()
  //TODO: check termination errors here!

  behavior of "POST /topic/"

  it should "create successfully" in {
    (for {
      resp <- createTopic()
    } yield {
      assert(resp.code == 201)
      val body = resp.body
      assert(body.topicId > 0)
      assert(body.postId > 0)
    }).unsafeToFuture()
  }

  it should "not create if the email is invalid" in {
    (for {
      resp <- createTopicRaw(createTopicRequest.copy(email = invalidEmail))
    } yield {
      assert(resp.code == 400)
    }).unsafeToFuture()
  }

  behavior of "POST /topic/${topicId}/post"

  it should "create successfully" in {
    (for {
      t    <- createTopic()
      resp <- createPost(t.body.topicId)
    } yield {
      assert(resp.code == 201)
      val body = resp.body
      assert(body.id > 0)
      assert(resp.getAuthHeader.isDefined)
    }).unsafeToFuture()
  }

  it should "not create if the email is invalid" in {
    (for {
      t    <- createTopic()
      resp <- createPostRaw(t.body.topicId, createPostRequest.copy(email = invalidEmail))
    } yield {
      assert(resp.code == 400)
    }).unsafeToFuture()
  }

  behavior of "GET /topic/?offset=${offset}&limit=${limit}"

  it should "keep an order of newly created topics" in {
    (for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t2.body.topicId)
      assert(tl2.id == t1.body.topicId)
    }).unsafeToFuture()
  }

  it should "move topic to the top when the new post is added" in {
    (for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      _      <- createPost(t1.body.topicId, createPostRequest.copy(text = "new post in old topic"))
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t1.body.topicId)
      assert(tl2.id == t2.body.topicId)
    }).unsafeToFuture()
  }

  it should "keep original order if post is first added and then deleted" in {
    (for {
      t1     <- createTopic(createTopicRequest.copy(subject = "topic 1"))
      t2     <- createTopic(createTopicRequest.copy(subject = "topic 2"))
      p      <- createPost(t1.body.topicId, createPostRequest.copy(text = "new post in old topic"))
      _      <- deletePost(t1.body.topicId, p.body.id, p.getAuthHeader.get)
      recent <- listTopics(0, 3)
    } yield {
      val tl1 :: tl2 :: _ = recent.body.topics
      assert(tl1.id == t2.body.topicId)
      assert(tl2.id == t1.body.topicId)
    }).unsafeToFuture()
  }

  behavior of "GET /topic/${topicId}/post/id?before=${before}&after=${after}"

  it should "return just the given row when no before and limit is specified" in {
    (for {
      t <- createTopic()
      p <- createPost(t.body.topicId)
      l <- listPosts(t.body.topicId, p.body.id)
    } yield {
      assert(l.body.posts.size == 1)
      assert(l.body.posts.head.id == p.body.id)
    }).unsafeToFuture()
  }

  it should "return both before and after if below limit" in {
    (for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), Some(2), Some(2))
    } yield {
      assert(l.body.posts.map(_.id) == ids.reverse)
    }).unsafeToFuture()
  }

  it should "return just before if below limit" in {
    (for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), Some(2), None)
    } yield {
      assert(l.body.posts.map(_.id) == ids.take(3).reverse)
    }).unsafeToFuture()
  }

  it should "return just after if below limit" in {
    (for {
      t <- createTopic()
      p <- List.fill(5)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      l <- listPosts(t.body.topicId, ids(2), None, Some(2))
    } yield {
      assert(l.body.posts.map(_.id) == ids.drop(2).reverse)
    }).unsafeToFuture()
  }

  it should "take former records if there is not enough after after roundup" in {
    (for {
      t <- createTopic()
      p <- List.fill(20)(createPost(t.body.topicId)).sequence
      ids = p.map(_.body.id)
      id  = ids(15)
      l <- listPosts(t.body.topicId, id, Some(5), Some(20))
    } yield {
      val idObj = l.body.posts.find(_.id == id)
      assert(idObj.isDefined)
      val after  = l.body.posts.filter(_.createdAt.isAfter(idObj.get.createdAt))
      val before = l.body.posts.filter(_.createdAt.isBefore(idObj.get.createdAt))
      println(before.size)
      println(after.size)
      assert(before.size == 5)
      assert(after.size == 4)
      //TODO: fixme
    }).unsafeToFuture()
  }

  it should "return error if numbers are negative" in {
    (for {
      l <- listPostsRaw(Long.MaxValue, Long.MaxValue, Some(-1), Some(-2))
    } yield {
      println(l)
      assert(l.code == 400)
    }).unsafeToFuture()
  }

  behavior of "PUT /topic/${topicId}/post/${postId}"

  it should "edit an existent post" in {
    (for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- editPost(t.body.topicId, p.body.id, p.getAuthHeader.get, "Updated post")
    } yield {
      assert(resp.code == 200)
    }).unsafeToFuture()
  }

  it should "not edit an existent post if there is a topicId mismatch" in {
    (for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- editPostRaw(t.body.topicId + 1, p.body.id, p.getAuthHeader.get, "Updated post")
    } yield {
      assert(resp.code == 304)
    }).unsafeToFuture()
  }

  it should "return valid codes if post doesn't exist" in {
    (for {
      resp <- editPostRaw(Long.MaxValue, Long.MaxValue, randomToken, "Updated post")
    } yield {
      assert(resp.code == 403)
    }).unsafeToFuture()
  }

  behavior of "DELETE /topic/${topicId}/post/${postId}"

  it should "delete an existent post" in {
    (for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePost(t.body.topicId, p.body.id, p.getAuthHeader.get)
    } yield {
      assert(resp.code == 200)
    }).unsafeToFuture()
  }

  it should "not delete an existent post if there is a topicId mismatch" in {
    (for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePostRaw(t.body.topicId + 1, p.body.id, p.getAuthHeader.get)
    } yield {
      assert(resp.code == 304)
    }).unsafeToFuture()
  }

  it should "return good code if the token is invalid" in {
    (for {
      t    <- createTopic()
      p    <- createPost(t.body.topicId)
      resp <- deletePostRaw(t.body.topicId, p.body.id, randomToken)
    } yield {
      assert(resp.code == 403)
    }).unsafeToFuture()
  }

  it should "return valid codes if post doesn't exist" in {
    (for {
      resp <- deletePostRaw(Long.MaxValue, Long.MaxValue, randomToken)
    } yield {
      assert(resp.code == 403)
    }).unsafeToFuture()
  }

  private val randomToken  = UUID.randomUUID()
  private val invalidEmail = "invalid email"

}
