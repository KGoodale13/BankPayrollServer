package controllers

import com.mongodb.casbah.Imports._
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.test._
import context.ApplicationContext
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, WithApplication}


/**
  * Test case for the [[controllers.ApplicationController]] class.
  */
class ApplicationControllerSpec extends PlaySpec with MockitoSugar with MongoEmbedDatabase with BeforeAndAfter {

  // Our mongodb test instance
  var mongoProps: MongodProps = _


  // Init the mongodb embedded server for our tests
  before {

    mongoProps = mongoStart( port = 27017 )

    Thread.sleep( 5000L ) // Wait for embedded mongo server to start

    // connect and insert test data
    val mongoClient =  MongoClient("localhost", 27017)("cos420")("companies")

    val builder = mongoClient.initializeOrderedBulkOperation
    builder.insert(
      MongoDBObject(
        "_id" -> 1,
        "uID" -> "idOfCompanyUserHasAccessTo",
        "companyName" -> "Authorized company",
        "address" -> "123 Maine st, Orono ME 04473",
        "employees" -> MongoDBList(
          MongoDBObject(
            "uID" -> "abcdedf-324fs-fsd24w24",
            "firstName" -> "Kyle",
            "lastName" -> "Goodale",
            "payType" -> "SALARY",
            "payRate" -> 9001
          ),
          MongoDBObject(
            "uID" -> "hghgk-789hgj-gj5fwerrwohj",
            "firstName" -> "John",
            "lastName" -> "Doe",
            "payType" -> "HOURLY",
            "payRate" -> 8999
          )
        ),
        "authorizedEmails" -> MongoDBList(
          "test@test.com",
          "someone@here.com"
        ),
        "payInterval" -> "WEEKLY",
        "payPeriodStart" -> "04/01/2018" // The start of the current pay period the company is in the format MM/DD/YYYY, because the company data is updated many times per day this will remain up to date
        )
    )
    builder.insert(
      MongoDBObject(
        "_id" -> 2,
        "uID" -> "IdOfCompanyUserDoesNotHaveAccessTo",
        "companyName" -> "",
        "address" -> "341 idk ln, Miami FL 33101",
        "employees" -> MongoDBList(
          MongoDBObject(
            "uID" -> "lqwodsa-324fs-fsdfsewef",
            "firstName" -> "Some",
            "lastName" -> "Dude",
            "payType" -> "SALARY",
            "payRate" -> 9001
          ),
          MongoDBObject(
            "uID" -> "soofmksdkj-423423d-fwefwefw",
            "firstName" -> "John",
            "lastName" -> "Schmoe",
            "payType" -> "HOURLY",
            "payRate" -> 8999
          )
        ),
        "authorizedEmails" -> MongoDBList(
          "test@noaccess.com"
        ),
        "payInterval" -> "WEEKLY",
        "payPeriodStart" -> "04/05/2018" // The start of the current pay period the company is in the format MM/DD/YYYY, because the company data is updated many times per day this will remain up to date
      )
    )

    builder.execute()

  }

  //
  after {
    mongoStop(mongoProps)
  }

  "The `getUserCompanies` action" should {

    "return unauthorized if user is unauthorized" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("invalid", "invalid") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getUserCompanies().apply(request)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return company data AS JSON if user is authorized" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("email", "test@test.com") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getUserCompanies().apply(request)

        status(result) mustBe OK
        contentType( result ) mustBe Some("application/json")
      }
    }

    "return not found when fetching employee data if user is not authorized" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("email", "test@test.com") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getCompanyEmployees("IdOfCompanyUserDoesNotHaveAccessTo").apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return unauthorized when fetching employee data if user is not logged in" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("email", "invalid@login.com") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getCompanyEmployees("IdOfCompanyUserDoesNotHaveAccessTo").apply(request)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return not found when fetching employee data if company doesn't exist" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("email", "test@test.com") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getCompanyEmployees("invalid_company_id").apply(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return employee data as JSON if user is idOfCompanyUserHasAccessTo" in new ApplicationContext {
      new WithApplication( application ) {

        val request = FakeRequest().withAuthenticator( LoginInfo("email", "test@test.com") )
        val controller = app.injector.instanceOf[ApplicationController]

        val result = controller.getCompanyEmployees("idOfCompanyUserHasAccessTo").apply(request)

        status(result) mustBe OK
        contentType( result ) mustBe Some("application/json")
      }
    }

  }

}
