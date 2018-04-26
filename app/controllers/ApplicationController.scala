package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.SilhouetteProvider
import models.daos._
import models.services.UserService
import play.api.Configuration
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.auth.AuthEnv
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

import scala.concurrent.{ExecutionContext, Future}


class ApplicationController @Inject() (
                                        //userService: UserService,
                                        cc: ControllerComponents,
                                        silhouetteProvider: SilhouetteProvider[AuthEnv],
                                        companyDAO: CompanyDAO,
                                        payrollDAO: PayrollDAO,
                                        configuration: Configuration,
) (implicit exec: ExecutionContext) extends AbstractController(cc) {

  import models.daos.CompanyJSONFormats._
  import models.daos.PayrollJSONFormats._



  // Helper that will validate the request json or return an error for us
  def validateJson[A : Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

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


  /**
    * Fetches all the past payroll periods currently stored. This is usually 30 days worth
    * @return All pay periods stored
    */
  def payrollSubmissions( companyId: String ) = silhouetteProvider.SecuredAction.async { request =>
    companyDAO.userIsAuthorized( request.identity.email.getOrElse("INVALID"), companyId ).flatMap {
      case true =>
        payrollDAO.getPayrollSubmissions( companyId ).map { payrollSubmissions =>
          Ok(Json.toJson(payrollSubmissions))
        }
      case false =>
        Future.successful( Unauthorized("You don't have access to view this companies data") )
    }

  }


  private[ApplicationController] val dateTimeFormat = DateTimeFormat.forPattern("MM/dd/yyyy")
  private[ApplicationController] case class RawPayrollSubmission( payPeriodStart: String, payroll: Seq[PayrollEntry] )
  private[ApplicationController] implicit val payrollRawSubmissionReads = Json.format[ RawPayrollSubmission ]


  /**
    * Called when a user attempts to submit payroll information for a pay period
    * @return 200 OK on success, error code otherwise
    */
  def submitPayroll( companyId: String) = silhouetteProvider.SecuredAction.async( validateJson[RawPayrollSubmission] ) { request =>
    companyDAO.getCompanyIfUserIsAuthorized(request.identity.email.getOrElse("INVALID"), companyId).flatMap {
      case Some(company) =>
        // User is authorized, next step is to verify that the payroll submission deadline has not passed ( this is 2 days before the start of the next pay period )

        val payPeriodStartDate = DateTime.parse(request.body.payPeriodStart, dateTimeFormat)
        val payrollSubmissionDeadline = Payroll.getLastDayForPayrollSubmission(payPeriodStartDate, company.payInterval)

        // Check if the deadline has passed
        if (DateTime.now().isBefore(payrollSubmissionDeadline))
        // Create the payroll submission object and submit it
          payrollDAO.submitPayroll(
            PayrollSubmission(
              companyId = companyId,
              payPeriodStart = request.body.payPeriodStart,
              dateSubmitted = DateTime.now.toString(dateTimeFormat),
              submittedBy = request.identity.email.get,
              payroll = request.body.payroll
            )
          ).map(
            if (_) Ok("Payroll submitted")
            else InternalServerError("Something went wrong while submitting your payroll request. Please try again later.")
          )
        else
          Future.successful(Gone("The payroll submission deadline for this pay period has passed"))

      case None =>
        Future.successful(Unauthorized("You don't have access to edit this company"))
    }

  }


}
