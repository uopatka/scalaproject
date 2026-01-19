package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import play.api.test.Helpers._
import play.api.test._
import play.api.test.CSRFTokenHelper._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import slick.jdbc.H2Profile.api._
import helpers.TestDatabaseHelper

/**
 
 */
class EntryControllerYearBugSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with MockitoSugar with BeforeAndAfterAll {

  lazy val testDb: Database = TestDatabaseHelper.createDatabase()

  override def fakeApplication(): Application = {
    Await.result(TestDatabaseHelper.createTables(testDb), 10.seconds)
    Await.result(TestDatabaseHelper.setupTestData(testDb), 10.seconds)

    val mockOpenLib = mock[services.OpenLibraryService]
    val mockCrossref = mock[services.CrossrefService]
    when(mockOpenLib.fetchByIsbn(ArgumentMatchers.anyString())).thenReturn(Future.successful(None))
    when(mockCrossref.fetchByDoi(ArgumentMatchers.anyString())).thenReturn(Future.successful(None))

    GuiceApplicationBuilder()
      .configure(
        "db.driver" -> "org.h2.Driver",
        "db.url" -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
        "db.user" -> "sa",
        "db.password" -> "",
        "db.connectionPool" -> "disabled",
        "app.uploads.dir" -> "./test-uploads"
      )
      .overrides(
        bind[Database].toInstance(testDb),
        bind[services.OpenLibraryService].toInstance(mockOpenLib),
        bind[services.CrossrefService].toInstance(mockCrossref)
      )
      .build()
  }

  override def afterAll(): Unit = {
    testDb.close()
    super.afterAll()
  }

  
  "EntryController updateBookYear BUG REPRODUCTION" should {

    "FAIL when form sends 'year' (as the view does)" in {
      // This reproduces the actual bug - form sends "year" but controller expects "publishYear"
      val request = FakeRequest(POST, "/book/updateYear/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("year" -> "2020")  // Form uses "year"
        .withCSRFToken
      
      val result = route(app, request).get
      
      // BUG: This returns BadRequest because controller looks for "publishYear"
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Nieprawidłowy rok")
    }

    "SUCCEED when form sends 'publishYear' (what controller expects)" in {
      // This shows how it SHOULD work if the form used the correct field name
      val request = FakeRequest(POST, "/book/updateYear/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("publishYear" -> "2020")
        .withCSRFToken
      
      val result = route(app, request).get
      
      // This should redirect on success
      status(result) mustBe SEE_OTHER
    }

    "FAIL when publishYear is empty string" in {
      // Even with correct field name, empty string fails because "".toInt throws exception
      val request = FakeRequest(POST, "/book/updateYear/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("publishYear" -> "")  // Empty string
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
    }

    "handle year = 0 correctly" in {
      // Year = 0 should be valid (means "unknown")
      val request = FakeRequest(POST, "/book/updateYear/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("publishYear" -> "0")
        .withCSRFToken
      
      val result = route(app, request).get
      
      // 0 is valid (>= 0 and <= current year)
      status(result) mustBe SEE_OTHER
    }
  }

  /**
   * Similar bug with page count
   */
  "EntryController updateBookPages BUG REPRODUCTION" should {

    "FAIL when form sends 'pageCount' (as the view does)" in {
      val request = FakeRequest(POST, "/book/updatePages/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("pageCount" -> "300")  // Form uses "pageCount"
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
    }

    "SUCCEED when form sends 'pages' (what controller expects)" in {
      val request = FakeRequest(POST, "/book/updatePages/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("pages" -> "300")  // Controller expects "pages"
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe SEE_OTHER
    }
  }
}
