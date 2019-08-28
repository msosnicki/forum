package com.ssn.forum.domain

import java.util.UUID

import com.ssn.forum.PostId

case class PostSecurity(token: UUID, _postId: PostId)
