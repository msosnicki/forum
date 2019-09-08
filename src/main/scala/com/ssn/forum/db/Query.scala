package com.ssn.forum.db

import com.ssn.forum.domain.{Email, Nickname, Subject, Topic}
import com.ssn.forum.{PostId, Token, TopicId}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits.UuidType

object Query extends CustomDbTypes {
  object Topics {
    def insert(subject: Subject): Update0 =
      sql"insert into topic (subject) values ($subject)".update
    def listLastActive(offset: Long, limit: Int): Query0[Topic.WithLastPostDate] =
      sql"""select * from (
           |	select distinct on (topic.id)
           |	    topic.id as topic_id, topic.subject, post.created_at as last_modified
           |	from
           |	    topic
           |	    inner join
           |	    post on post.topic_id = topic.id
           |	order by topic.id, last_modified desc
           |) topics 
           |order by topics.last_modified desc
           |offset $offset limit $limit
           |""".stripMargin.query[Topic.WithLastPostDate]
  }

  object Posts {

    def insert(topicId: TopicId, text: String, nickname: Nickname, email: Email): Update0 =
      sql"insert into post (topic_id, text, nickname, email) values ($topicId, $text, $nickname, $email)".update
    def edit(topicId: TopicId, postId: PostId, text: String): Update0 =
      sql"update post set text = $text where topic_id = $topicId and id = $postId".update
    def delete(topicId: TopicId, postId: PostId): Update0 =
      sql"delete from post where topic_id = $topicId and id = $postId".update
    def listPostsAround(
        topicId: TopicId,
        postId: PostId,
        before: Int,
        after: Int
    ): Query0[PersistentPost] = {
      val fragments = List(
        selectBeforePost(topicId, postId, before),
        selectPost(topicId, postId),
        selectAfterPost(topicId, postId, after)
      )
      val all = fragments.reduce((a, b) => fr"(" ++ a ++ fr")" ++ fr0" UNION ALL " ++ fr"(" ++ b ++ fr")") ++ fr"order by createdAt desc"
      all.query[PersistentPost]
    }

    private val selectWholePost =
      fr"select id, text, nickname, email, created_at as createdAt, topic_id as _topicId from post"
    private def selectBeforePost(topicId: TopicId, postId: PostId, limit: Int) =
      selectWholePost ++ fr"where topic_id = $topicId and created_at < (select created_at from post where topic_id = $topicId and id=$postId) order by created_at desc limit $limit"

    private def selectAfterPost(topicId: TopicId, postId: PostId, limit: Int) =
      selectWholePost ++ fr"where topic_id = $topicId and created_at > (select created_at from post where topic_id = $topicId and id=$postId) order by created_at asc limit $limit"

    private def selectPost(topicId: TopicId, postId: PostId) =
      selectWholePost ++ fr"where topic_id = $topicId and id = $postId"
  }

  object PostSecurity {
    def insert(postId: PostId): Update0 =
      sql"insert into post_security (post_id) values ($postId)".update
    def insert(postId: PostId, token: Token): Update0 =
      sql"insert into post_security (post_id, token) values ($postId, $token)".update
    def get(postId: PostId): Query0[Token] =
      sql"select token from post_security where post_id=$postId".query[Token]
    def delete(postId: PostId): Update0 =
      sql"delete from post_security where post_id=$postId".update
  }
}
