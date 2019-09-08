package com.ssn.forum

import com.ssn.forum.domain._

import scala.collection.generic.CanBuildFrom

package object db {
  type PersistentTopic           = PersistentEntity[TopicId, Topic]
  type PersistentPost            = PersistentEntity[PostId, Post]
  type Collection[Elem, Coll[_]] = CanBuildFrom[Nothing, Elem, Coll[Elem]]
}
