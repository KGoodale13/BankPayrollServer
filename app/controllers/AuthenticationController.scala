package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import forms.{SignInForm, SignUpForm}
import models.User
import models.services.UserService
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
  * The credentials auth controller.
  *
  * @param userService The user service implementation
  * @param cc ControllerComponents
  * @param authenticatorService The authentication service implementation for the current env
  * @param passwordHasher Our password hasher
  * @param credentialsProvider Database credentials provider
  * @param configuration Our play typesafe configuration
  * @param clock The clock instance.
  */
class AuthenticationController @Inject() (
  userService: UserService,
  cc: ControllerComponents,
  authenticatorService: AuthenticatorService[BearerTokenAuthenticator],
  authInfoRepository: AuthInfoRepository,
  passwordHasher: PasswordHasher,
  credentialsProvider: CredentialsProvider,
  configuration: Configuration,
  clock: Clock
) (implicit exec: ExecutionContext ) extends AbstractController(cc) {

  /**
    * Authenticates a user against the credentials provider.
    *
    * @return The result to display.
    */
  def authenticate = Action.async { implicit request =>
    SignInForm.form.bindFromRequest.fold (
      formWithErrors => Future.successful( Results.BadRequest("Invalid input") ),
      formData => {
        val credentials = Credentials(formData.email, formData.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case Some( user ) =>
              authenticatorService.create(loginInfo).flatMap { authenticator =>
                authenticatorService.init(authenticator).map { token =>
                  Results.Ok(token)
                }
              }
            case None => Future.successful( Results.Unauthorized("User not found") )
          }
        }.recover {
          case e: ProviderException =>
            Results.Unauthorized("Invalid username/password combination")
        }
      }
    )
  }


  def signUp = Action.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest("Invalid input")),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            Future.successful( UnprocessableEntity("User already exists") )
          case None =>

            val authInfo = passwordHasher.hash(data.password)
            val user = User(
              userID = UUID.randomUUID(),
              loginInfo = loginInfo,
              firstName = Some(data.firstName),
              lastName = Some(data.lastName),
              email = Some(data.email)
            )

            for {
              user <- userService.save(user)
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- authenticatorService.create(loginInfo)
              token <- authenticatorService.init(authenticator)
            } yield {
              Results.Ok(token)
            }
        }
      }
    )
  }




}