package com.ssn.forum.domain

import java.time.Instant

import com.ssn.forum.db.PersistentTopic

case class Topic(subject: String)

object Topic {
  case class WithLastPostDate(topic: PersistentTopic, lastPostTime: Instant)
}
