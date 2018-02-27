package models.daos


import java.text.SimpleDateFormat
import javax.inject._

import com.mohiva.play.silhouette.api.LoginInfo
import play.api.db.slick.DatabaseConfigProvider
import com.mohiva.play.silhouette.api.repositories.AuthenticatorRepository
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
  * The DAO to store the password information.
  */
@Singleton
class BearerTokenInfoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends AuthenticatorRepository[BearerTokenAuthenticator] with DAOSlick {

  import profile.api._

  // Logic for converting our datetime types to and from strings
  protected val dbDateTimeFormat = "YYYY-MM-dd HH:mm:ss"

  protected def formatDateTime(dateTime: DateTime): String = dateTime.toString(dbDateTimeFormat)

  protected def parseDateTime(dateTimeString: String): DateTime = DateTime.parse(dateTimeString)

  protected def addAction(authInfo: BearerTokenAuthenticator) =
    loginInfoQuery(authInfo.loginInfo).result.head.flatMap { dbLoginInfo =>
      dBBearerTokenInfos += DBBearerTokenInfo(
        authInfo.id,
        dbLoginInfo.id.get,
        formatDateTime(authInfo.lastUsedDateTime),
        formatDateTime(authInfo.expirationDateTime)
      )
    }.transactionally


  /**
    * Finds the auth info which is linked with the specified login info.
    *
    * @param id The token id
    * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
    */
  override def find(id: String): Future[Option[BearerTokenAuthenticator]] = {
    val query = for {
      bearerToken <- dBBearerTokenInfos if bearerToken.id === id
      loginInfo <- dbLoginInfos if loginInfo.id === bearerToken.loginInfoId
    } yield (bearerToken, loginInfo)

    db.run( query.result.headOption ).map { resultOption =>
      resultOption.map {
        case (bearerToken, loginInfo) =>
          BearerTokenAuthenticator(
            bearerToken.id,
            LoginInfo( loginInfo.providerID, loginInfo.providerKey),
            parseDateTime(bearerToken.lastUsed),
            parseDateTime(bearerToken.expiration),
            None
          )
      }
    }
  }

  /**
    * Adds new auth info
    *
    * @param authInfo The auth info to add.
    * @return The added auth info.
    */
  override def add(authInfo: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    db.run(addAction(authInfo)).map { _ => authInfo }
  }


  /**
    * Updates the auth info for the given login info.
    *
    * @param authInfo The auth info to update.
    * @return The updated auth info.
    */
  override def update(authInfo: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    val updateQuery = dBBearerTokenInfos.filter(_.id === authInfo.id)
      .map(dbAuthInfo => (dbAuthInfo.lastUsed, dbAuthInfo.expiration))
      .update((formatDateTime(authInfo.lastUsedDateTime), formatDateTime(authInfo.expirationDateTime)))
    db.run(updateQuery).map(_ => authInfo)
  }


  /**
    * Removes the auth info for the given login info.
    *
    * @param id The token id to remove
    * @return A future to wait for the process to be completed.
    */
  override def remove(id: String): Future[Unit] =
    db.run(dBBearerTokenInfos.filter(_.id === id).delete).map(_ => ())

}

