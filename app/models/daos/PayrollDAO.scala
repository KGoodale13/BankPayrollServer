package models.daos

import javax.inject._

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsPath, Json, Writes}

import scala.concurrent.{ExecutionContext, Future}
import play.modules.reactivemongo._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection._
import reactivemongo.play.json._
import play.api.libs.functional.syntax._
import scala.concurrent.duration.DurationInt



// Mongo Document format definitions

object Payroll {

  /**
    * Calculates the end date of the pay period given the start of the pay period and the pay cycles the company uses
    * @param payPeriodStart - The date of the start of the pay period
    * @param payCycle - The pay cycle used by the company: "WEEKLY", "BIWEEKLY", or "BIMONTHLY"
    * @return Instant - The date the pay period ends
    */
  def calculatePayPeriodEnd( payPeriodStart: DateTime, payCycle: String ): DateTime = {

    val payDay = payCycle match {
      case "WEEKLY" => // every week
        payPeriodStart.plusDays( 6 )
      case "BIWEEKLY" => // every 14 days
        payPeriodStart.plusDays( 13 )
      case "BIMONTHLY" => // BI-MONTHLY Pay periods are on the 1st to the 15th and then 16th to the end of the month
        val dayEnd = payPeriodStart.getDayOfMonth match {
          case 1 => 15
          case 16 => payPeriodStart.dayOfMonth().getMaximumValue
        }
        payPeriodStart.withDayOfMonth( dayEnd )
    }

    payDay.withHourOfDay( 23 ).withMinuteOfHour( 59 ).withSecondOfMinute( 59 )
  }

  /**
    * Calculates the last day payroll can be submitted. The end
    * @param payPeriodStart - The date of the start of the pay period
    * @param payCycle - The pay cycle used by the company: "WEEKLY", "BIWEEKLY", or "BIMONTHLY"
    * @return Instant - The last day payroll can be submitted by
    */
  def getLastDayForPayrollSubmission( payPeriodStart: DateTime, payCycle: String ): DateTime = {

    val payDay = payCycle match {
      case "WEEKLY" => // pay
        payPeriodStart.plusDays( 13 )
      case "BIWEEKLY" =>
        payPeriodStart.plusDays( 27 )
      case "BIMONTHLY" => // BI-MONTHLY Pay periods are on the 1st to the 15th and then 16th to the end of the month
        val dayEnd = payPeriodStart.getDayOfMonth match {
          case 1 => payPeriodStart.dayOfMonth().getMaximumValue
          case 16 => 15
        }
        payPeriodStart.withDayOfMonth( dayEnd )
    }

    // The last day for pay period submission is 2 days before the end of the next pay period @ 5PM
    payDay.minusDays(2).withHourOfDay( 17 ).withMinuteOfHour( 0 ).withSecondOfMinute( 0 )
  }
}

case class PayrollSubmission (
  companyId: String,
  payPeriodStart: String,
  dateSubmitted: String,
  submittedBy: String,
  payroll: Seq[PayrollEntry],
)

case class PayrollEntry (
  employeeId: String,
  hours: Double,
)


// Json formats for our mongo documents
object PayrollJSONFormats {
  implicit val payrollEntryReads = Json.format[PayrollEntry]
  implicit val payrollSubmissionFormat = Json.format[PayrollSubmission]
}

@Singleton
class PayrollDAO @Inject() ( implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi ) {

  import PayrollJSONFormats._

  // Quick reference to our companies collection
  val payrollCollection = reactiveMongoApi.database.map(_.collection("payroll"))


  /**
    * Fetches company data for a specific company if the user is authorized.
    * This is just a convenient function if you need to get company info and also need to authorize a user.
    */
  def getPayrollSubmissions( companyId: String ): Future[Seq[PayrollSubmission]] = {
    val query = Json.obj( "companyId" -> companyId )
    payrollCollection.flatMap(
      _.find(query)
      .cursor[PayrollSubmission](ReadPreference.primaryPreferred)
      .collect[Seq](100, Cursor.FailOnError[Seq[PayrollSubmission]]())
    )
  }

  /**
    * Inserts or updates a payroll submission
    * payrollSubmission - The Payroll submission object to upsert into the db
    */
  def submitPayroll( payrollSubmission: PayrollSubmission ): Future[ Boolean ] = {

    val query = Json.obj( "companyId" -> payrollSubmission.companyId, "payPeriodStart" -> payrollSubmission.payPeriodStart )

    val submission = Json.obj(
      "$set" -> Json.toJson( payrollSubmission )
    )

    payrollCollection.flatMap(
      _.update( query, submission, upsert = true )
      .map( _.ok )
    )

  }

}