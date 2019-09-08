package com.ssn.forum.domain

import java.time.Instant

import com.ssn.forum._

case class Post(text: String, nickname: Nickname, email: Email, createdAt: Instant, _topicId: TopicId)
