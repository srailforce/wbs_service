package models

import java.time.{LocalDate, LocalDateTime}

import models._
import models.User.Gender
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

class Repository(private val dbConfigProvider: DatabaseConfigProvider)
                (implicit val executionContext: ExecutionContext)
  extends RepositoryImplicits {
  self =>
  type Profile = PostgresProfile
  val dbConfig = dbConfigProvider.get[Profile]

  import dbConfig._
  import profile.api._

  private val users = TableQuery[UserTable]
  private val microBlogs = TableQuery[MicroBlogTable]
  private val comments = TableQuery[CommentTable]
  /**
    * TableRow for User schema
    * @param tag
    */
  private class UserTable(tag: Tag) extends Table[User](tag, "users") {

    override def * =
      (id.?, name, gender, password, email.?, birthday.?).shaped <> ((User.apply _).tupled, User.unapply)

    def id = column[Long]("user_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("username")

    def gender = column[Gender]("gender")

    def password = column[String]("password")

    def email = column[String]("email")

    def birthday = column[LocalDate]("birthday")
  }

  /**
    * TableRow for MicroBlog schema
    * @param tag
    */
  private class MicroBlogTable(tag: Tag) extends Table[MicroBlog](tag, "micro_blog") {
    override def * = (blogId.?, content, timestamp, userId) <> ((MicroBlog.apply _).tupled, MicroBlog.unapply)

    def blogId = column[Long]("blog_id", O.PrimaryKey, O.AutoInc)

    def content = column[String]("content")

    def timestamp = column[LocalDate]("timestamp")

    def userId = column[Long]("user_id")
  }

  /**
    * TableRow for Comment schema
    * @param tag
    */
  private class CommentTable(tag: Tag) extends Table[Comment](tag, "COMMENTS") {
    override def * = (id.?, blogId, content, stars, userId, timestamp) <> ((Comment.apply _).tupled, Comment.unapply)

    /**
      * primary key
      */
    def id = column[Long]("comment_id", O.PrimaryKey, O.AutoInc)

    def blogId = column[Long]("blog_id")

    def content = column[String]("content")

    def stars = column[Int]("stars", O.Default(0))

    def userId = column[Long]("user_id")

    def timestamp = column[LocalDateTime]("timestamp")
  }

  object Users {
    val users = self.users
    def create(user: User): Future[User] = create(user.name, user.gender, user.password, user.email, user.birthday)

    def create(name: String,
               gender: Gender,
               password: String,
               email: Option[String] = None,
               birthday: Option[LocalDate] = None): Future[User] =
      db.run {
        (users.map(p => (p.name, p.gender, p.password, p.email, p.birthday))
          returning users.map(_.id)
          into ((ut, id) => User(Some(id), ut._1, ut._2, ut._3, Option(ut._4), Option(ut._5)))
          ) += (name, gender, password, email.orNull, birthday.orNull)
      }

    def list(): Future[Seq[User]] = db.run {
      users.result
    }

    def delete(id: Long): Future[Int] = db.run {
      users.filter(_.id === id).delete
    }

    def find(id: Long): Future[Option[User]] = db.run {
      users.filter(_.id === id).result
    } map (_.headOption)

    def find(name: String): Future[Option[User]] = db.run {
      users.filter(_.name === name).result
    } map (_.headOption)
  }

  object MicroBlogs {
    val microBlogs = self.microBlogs

    def create(microBlog: MicroBlog): Future[MicroBlog] = db.run {
      (microBlogs.map(mb => (mb.content, mb.timestamp, mb.userId))
        returning microBlogs.map(_.blogId)
        into { case ((content, timestamp, userId), blogId) => MicroBlog(Some(blogId), content, timestamp, userId) }
        ) += (microBlog.content, microBlog.timestamp, microBlog.userId)
    }


    def findByUserId(userId: Long): Future[Seq[MicroBlog]] = db.run {
      microBlogs.filter(_.userId === userId).sortBy(_.timestamp).result
    }

    def findByBlogId(blogId: Long): Future[Option[MicroBlog]] = db.run {
      microBlogs.filter(_.blogId === blogId).result
    }.map(_.headOption)

    def delete(id: Long): Future[Int] = db.run {
      microBlogs.filter(_.blogId === id).delete
    }

  }

  object Comments {
    val comments = self.comments
    def create(comment: Comment): Future[Comment] = db.run {
      (comments.map(c => (c.blogId, c.content, c.userId))
        returning comments.map(c => (c.id, c.stars, c.timestamp))
        into { case ((blogId, content, userId), (commentId, stars, timestamp)) =>
        Comment(Some(commentId), blogId, content, stars, userId, timestamp) }
        ) += (comment.blogId, comment.content, comment.userId)
    }

    def findByBlogId(id: Long): Future[Seq[Comment]] = db.run {
      comments.filter(_.id === id).sortBy(_.timestamp.desc).result
    }

    def delete(id: Long): Future[Int] = db.run {
      comments.filter(_.id === id).delete
    }
  }
}