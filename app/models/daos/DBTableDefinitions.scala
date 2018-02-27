package models.daos


import java.time.Instant
import java.util.Date

import com.mohiva.play.silhouette.api.LoginInfo
import org.joda.time.DateTime
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf

trait DBTableDefinitions {

  protected val driver: JdbcProfile

  import driver.api._

  // Our User information table and case class
  case class DBUser (
    userID: String,
    firstName: Option[String],
    lastName: Option[String],
    email: Option[String]
  )

  class Users(tag: Tag) extends Table[DBUser](tag, "User") {
    def id = column[String]("id", O.PrimaryKey)
    def firstName = column[Option[String]]("firstName")
    def lastName = column[Option[String]]("lastName")
    def email = column[Option[String]]("email")
    def * = (id, firstName, lastName, email) <> (DBUser.tupled, DBUser.unapply)
  }

  // User Login info mappings
  case class DBLoginInfo (
     id: Option[Long],
     providerID: String,
     providerKey: String
   )

  class LoginInfos(tag: Tag) extends Table[DBLoginInfo](tag, "logininfo") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def providerID = column[String]("providerID")
    def providerKey = column[String]("providerKey")
    def * = (id.?, providerID, providerKey) <> (DBLoginInfo.tupled, DBLoginInfo.unapply)
  }


  // This maps user bearer tokens to users for our Bearer Token authentication
  case class DBUserLoginInfo (
     userID: String,
     loginInfoId: Long
   )

  class UserLoginInfos(tag: Tag) extends Table[DBUserLoginInfo](tag, "userlogininfo") {
    def userID = column[String]("userID")
    def loginInfoId = column[Long]("loginInfoId")
    def * = (userID, loginInfoId) <> (DBUserLoginInfo.tupled, DBUserLoginInfo.unapply)
  }

  // Password storage table. The advantage of this over storing it in the user table is we can change our
  // Encryption algorithms on the fly and update users as they login and also allow other types of authentication
  case class DBPasswordInfo (
    hasher: String,
    password: String,
    salt: Option[String],
    loginInfoId: Long
  )

  class PasswordInfos(tag: Tag) extends Table[DBPasswordInfo](tag, "passwordinfo") {
    def hasher = column[String]("hasher")
    def password = column[String]("password")
    def salt = column[Option[String]]("salt")
    def loginInfoId = column[Long]("loginInfoId")
    def * = (hasher, password, salt, loginInfoId) <> (DBPasswordInfo.tupled, DBPasswordInfo.unapply)
  }

  // Bearer token authenticator backing store
  case class DBBearerTokenInfo (
     id: String,
     loginInfoId: Long,
     lastUsed: String,
     expiration: String
   )

  class BearerTokenInfos(tag: Tag) extends Table[DBBearerTokenInfo](tag, "authtokeninfo") {
    def id = column[String]("id", O.PrimaryKey)
    def loginInfoId = column[Long]("loginInfoId")
    def lastUsed = column[String]("lastUsed")
    def expiration = column[String]("expiration")
    def * = (id, loginInfoId, lastUsed, expiration) <> (DBBearerTokenInfo.tupled, DBBearerTokenInfo.unapply(_))
  }


  // Table definitions
  val dbUsers = TableQuery[Users]
  val dbLoginInfos = TableQuery[LoginInfos]
  val dbUserLoginInfos = TableQuery[UserLoginInfos]
  val dbPasswordInfos = TableQuery[PasswordInfos]
  val dBBearerTokenInfos = TableQuery[BearerTokenInfos]

  // Queries used in more than one place

  def loginInfoQuery(loginInfo: LoginInfo) =
    dbLoginInfos.filter(dbLoginInfo => dbLoginInfo.providerID === loginInfo.providerID && dbLoginInfo.providerKey === loginInfo.providerKey)



}