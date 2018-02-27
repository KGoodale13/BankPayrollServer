package modules


import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.util.{PlayCacheLayer, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.password.{BCryptPasswordHasher, BCryptSha256PasswordHasher}
import models.daos.{BearerTokenInfoDAO, PasswordInfoDAO, UserDAO, UserDAOImpl}
import models.services.{UserService, UserServiceImpl}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.ws.WSClient
import utils.auth.AuthEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration


/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `modules.Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
  */
class SilhouetteModule extends AbstractModule {

   override def configure() = {
    // Create instances of our dependency injected classes
    bind(classOf[Clock]).toInstance(Clock())

    bind(classOf[UserDAO]).to(classOf[UserDAOImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])

    bind(classOf[PasswordHasher]).toInstance( new BCryptSha256PasswordHasher() )

    bind(classOf[IDGenerator]).toInstance(new SecureRandomIDGenerator(128))
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(classOf[CacheLayer]).to(classOf[PlayCacheLayer])
  }

  /**
    * Provides the HTTP layer implementation.
    *
    * @param client Play's WS client.
    * @return The HTTP layer implementation.
    */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
    * Provides the Password Hasher registry
    *
    * @return PasswordHasher Registry
    */
  @Provides
  def providePasswordHasherRegistry( currentPasswordHasher: PasswordHasher ): PasswordHasherRegistry = {
    PasswordHasherRegistry(
      current = currentPasswordHasher,
      deprecated = Seq()
    )
  }


  /**
    * Provides the Silhouette environment.
    *
    * @param userService The user service implementation.
    * @param authenticatorService The authentication service implementation.
    * @param eventBus The event bus instance.
    * @return The Silhouette environment.
    */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[BearerTokenAuthenticator],
    eventBus: EventBus): Environment[AuthEnv] = {

    Environment[AuthEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }



  /**
    * Provides the authenticator service.
    *
    * @param idGenerator The ID generator implementation.
    * @param configuration The Play configuration.
    * @param clock The clock instance.
    * @return The authenticator service.
    */
  @Provides
  def provideAuthenticatorService(
    dbConfigProvider: DatabaseConfigProvider,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[BearerTokenAuthenticator] = {

    val config = BearerTokenAuthenticatorSettings(
      authenticatorIdleTimeout = Some(configuration.get[FiniteDuration]("silhouette.authenticator.authenticatorIdleTimeout")),
      fieldName = configuration.get[String]("silhouette.authenticator.fieldName"),
      authenticatorExpiry = configuration.get[FiniteDuration]("silhouette.authenticator.authenticatorExpiry")
    )

    new BearerTokenAuthenticatorService( config, new BearerTokenInfoDAO( dbConfigProvider ), idGenerator, clock)
  }

  /**
    * Provides the auth info repository.
    *
    * @param dbConfigProvider Slick database configuration
    * @return The auth info repository instance.
    */
  @Provides
  def provideAuthInfoRepository( dbConfigProvider: DatabaseConfigProvider ): AuthInfoRepository =
    new DelegableAuthInfoRepository( new PasswordInfoDAO( dbConfigProvider ) )


}
