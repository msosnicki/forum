package com.ssn.forum.service

import cats.implicits._
import cats.effect.Sync
import cats.~>
import com.ssn.forum._
import com.ssn.forum.db.{PersistentTopic, Repository}
import com.ssn.forum.domain.Topic

final class TopicService[DB[_], F[_]](repo: Repository.Topics[DB], transact: DB ~> F)(implicit S: Sync[F])
    extends TopicService.Algebra[F] {
  override def createTopic(subject: String): F[TopicId] =
    transact(repo.insert(subject)).map(_.id)

  override def listLastActive(offset: TopicId, limit: Int): F[List[Topic.WithLastPostDate]] =
    transact(repo.listLastActive(offset, limit))
}

object TopicService {
  abstract class Algebra[F[_]] {
    def createTopic(subject: String): F[TopicId]
    def listLastActive(offset: Long, limit: Int): F[List[Topic.WithLastPostDate]]
  }
}
