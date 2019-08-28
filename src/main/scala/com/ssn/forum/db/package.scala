package com.ssn.forum

import com.ssn.forum.domain._

package object db {
  type PersistentTopic = PersistentEntity[TopicId, Topic]
  type PersistentPost  = PersistentEntity[PostId, Post]
}
