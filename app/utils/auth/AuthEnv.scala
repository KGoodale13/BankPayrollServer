package utils.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import models.User

// production auth environment
trait AuthEnv extends Env {
  type I = User
  type A = BearerTokenAuthenticator
}