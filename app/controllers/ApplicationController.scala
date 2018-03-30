package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.SilhouetteProvider
import models.daos.CompanyDAO
import models.services.UserService
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.auth.AuthEnv

import scala.concurrent.ExecutionContext


class ApplicationController @Inject() (
                                        userService: UserService,
                                        cc: ControllerComponents,
                                        silhouetteProvider: SilhouetteProvider[AuthEnv],
                                        companyDAO: CompanyDAO,
                                        configuration: Configuration,
) (implicit exec: ExecutionContext ) extends AbstractController(cc) {

  import models.daos.CompanyJSONFormats._

  /**
    * Returns an array of all the companies the user is allowed to access
    *
    * @return Array of companies in the format { uID: String, companyName: String, address: String }
    */
  def getUserCompanies() = silhouetteProvider.SecuredAction.async { request =>
    companyDAO.getAllAuthorized( request.identity.email.get ).map( companies =>
      Ok(Json.toJson(companies))
    )
  }

  /**
    * Returns an array of all employees at the company
    * @param id - The companyId
    */
  def getCompanyEmployees( id: String ) = silhouetteProvider.SecuredAction.async { request =>
    companyDAO.getCompanyEmployeesIfAuthorized( request.identity.email.get, id ).map {
      case Some( employeeList ) => Ok( Json.toJson( employeeList ) )
      case None => NotFound("Not Found")
    }
  }

}
