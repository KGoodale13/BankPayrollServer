package context

import java.util.UUID

import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.typesafe.config.ConfigFactory
import models.User
import org.specs2.specification.Scope
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoApi}
import reactivemongo.api.{MongoConnection, MongoDriver}
import utils.auth.AuthEnv


// The application context for our tests


trait ApplicationContext extends Scope {


  class FakeModule extends AbstractModule {
    def configure() = {
      bind(classOf[Environment[AuthEnv]]).toInstance(env)
      bind(classOf[Configuration]).toInstance( configuration )
    }


    @Provides
    def provideEnvironment(): Environment[AuthEnv] = env

  }

  /**
    * An identity.
    */
  val identity = User(
    userID = UUID.randomUUID(),
    loginInfo = LoginInfo("email", "test@test.com"),
    firstName = Some("Kyle"),
    lastName = Some("Goodale"),
    email = Some("test@test.com")
  )

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val configuration: Configuration = Configuration(ConfigFactory.load("application.test.conf"))

  /**
    * A Silhouette fake environment.
    */
  implicit val env: Environment[AuthEnv] = new FakeEnvironment[AuthEnv](Seq(identity.loginInfo -> identity))

  lazy val application = new GuiceApplicationBuilder()
    .overrides(new FakeModule)
    .loadConfig(configuration)
    .build()

}