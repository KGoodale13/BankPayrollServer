package models.daos

import javax.inject._

import play.api.libs.json.{JsObject, JsPath, Json, Writes}

import scala.concurrent.{ExecutionContext, Future}
import play.modules.reactivemongo._
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection._
import reactivemongo.play.json._
import play.api.libs.functional.syntax._

// Mongo Document format definitions

case class Employee (
  uID: String,
  firstName: String,
  lastName: String,
  payType: String,
  payRate: Double,
)

case class Company (
   uID: String,
   companyName: String,
   address: String,
   employees: Seq[Employee],
   authorizedEmails: Seq[String]
)

// Json formats for our mongo documents
object CompanyJSONFormats {
  implicit val employeeFormat = Json.format[Employee]
  implicit val companyReads = Json.reads[Company]

  // Custom writer because we don't want to write all the data in responses. Only the necessary data
  implicit val companyWrites: Writes[Company] = (
    (JsPath \ "uID").write[String] and
    (JsPath \ "companyName").write[String] and
    (JsPath \ "address").write[String]
  )( company => (company.uID, company.companyName, company.address) )

}

@Singleton
class CompanyDAO @Inject() ( implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi ) {

  import CompanyJSONFormats._

  // Quick reference to our companies collection
  val companiesCollection = reactiveMongoApi.database.map(_.collection("companies"))

  /**
    * Returns data for all companies the email is authorized to access
    * @param authorizedEmail - Email of the user to find authorized companies for
    *
    */
  def getAllAuthorized( authorizedEmail: String ): Future[Seq[Company]] = {
    val query = Json.obj( "authorizedEmails" -> authorizedEmail )
    companiesCollection.flatMap(
      _.find(query)
      .cursor[Company](ReadPreference.primaryPreferred)
      .collect[Seq](100, Cursor.FailOnError[Seq[Company]]())
    )
  }

  /**
    * Returns a list of all employees at the company as long as the email passed is authorized to view the companies data
    * @param authorizedEmail - Email of the user requesting the data. Used to make sure they are authorized to view the data
    * @param companyId - The company whose employees to return
    */
  def getCompanyEmployeesIfAuthorized( authorizedEmail: String, companyId: String ): Future[Option[Seq[Employee]]] = {
    val query = Json.obj( "uID" -> companyId, "authorizedEmails" -> authorizedEmail )
    val filter = Json.obj( "Employees" -> 1 )
    // Attempt to find a matching company, in the event we do map the return value to only the employee seq of the company
    companiesCollection.flatMap(
      _.find(query)
      .one[Company]
    ).map {
      case Some(company) => Some( company.employees )
      case _ => None
    }
  }

}


/**

JSON Generator code for test data
Can be used at next.json-generator.com

[
  {
    'repeat(20, 50)':
    {
      uID: '{{guid()}}',
      companyName: '{{company()}}',
      address: '{{integer(100, 999)}} {{street()}}, {{city()}}, {{state()}},{{integer(100, 10000)}}',
      employees: [
        {
          'repeat(2, 13)':
          {
            uID: '{{guid()}}',
            firstName: '{{firstName()}}',
            lastName: '{{surname()}}',
            address: '{{integer(100, 999)}} {{street()}}, {{city()}}, {{state()}},{{integer(100, 10000)}}',
            payType: '{{random("HOURLY", "SALARY_WEEKLY")}}', // Can add others in the future
            payRate: function( tags, parent, index ) {
              switch( this.payType ) {
                case "HOURLY":
                  return tags.floating( 10, 75, 2 );
                case "SALARY_WEEKLY":
                  return tags.floating( 400, 3000, 2 );
                default:
                  return tags.floating( 400, 3000, 2 );
              }
            }
          }
        }
      ],
      authorizedEmails: [{
        'repeat(1,3)': function(tags){
          return tags.firstName() + "." + surname() + "@" + this.companyName.toLowerCase() + tags.domainZone();
        }
      }]
    }
  }
]

  */