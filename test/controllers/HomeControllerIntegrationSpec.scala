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
 * Integration tests for HomeController using H2 in-memory database.
 * These tests verify the controller behavior with a real database.
 */
class HomeControllerIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with MockitoSugar with BeforeAndAfterAll {

  // Create a shared H2 database for all tests
  lazy val testDb: Database = TestDatabaseHelper.createDatabase()

  override def fakeApplication(): Application = {
    // Initialize tables before building the app
    Await.result(TestDatabaseHelper.createTables(testDb), 10.seconds)
    Await.result(TestDatabaseHelper.setupTestData(testDb), 10.seconds)

    // Mock external services (they call external APIs)
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
        "app.uploads.dir" -> "./test-uploads",
        "play.filters.disabled" -> List("play.filters.csrf.CSRFFilter")
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

  "HomeController GET (Integration)" should {

    "render the index page with status 200" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Bugshelv")
    }

    "render page with 'Brak książek' message for guest user (no entries for userId 0)" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      // Guest (no session) should see entries for userId = 0, which we didn't add in setupTestData
      contentAsString(home) must include("Brak książek")
    }

    "render page with books for logged-in user" in {
      // Simulate logged-in user with session
      val request = FakeRequest(GET, "/")
        .withSession("userId" -> "1", "username" -> "testuser")
      val home = route(app, request).get

      status(home) mustBe OK
      // User 1 has 2 books in setupTestData
      contentAsString(home) must include("1984")
      contentAsString(home) must include("To Kill a Mockingbird")
    }

    "render book details page" in {
      val request = FakeRequest(GET, "/book/9780141036144")
        .withSession("userId" -> "1", "username" -> "testuser")
      val bookPage = route(app, request).get

      status(bookPage) mustBe OK
      contentAsString(bookPage) must include("1984")
      contentAsString(bookPage) must include("George Orwell")
    }

    "return page with add book form" in {
      val request = FakeRequest(GET, "/add-book").withCSRFToken
      val addBookPage = route(app, request).get

      status(addBookPage) mustBe OK
      contentAsString(addBookPage) must include("isbn")
    }
  }

  "HomeController filtering (Integration)" should {

    "filter books by status" in {
      val request = FakeRequest(GET, "/?status=to_read")
        .withSession("userId" -> "1", "username" -> "testuser")
      val filtered = route(app, request).get

      status(filtered) mustBe OK
      // Default status is 'to_read', so both books should appear
      contentAsString(filtered) must include("1984")
    }

    "search books by title" in {
      val request = FakeRequest(GET, "/?search=1984")
        .withSession("userId" -> "1", "username" -> "testuser")
      val searched = route(app, request).get

      status(searched) mustBe OK
      contentAsString(searched) must include("1984")
      contentAsString(searched) must not include("To Kill a Mockingbird")
    }
  }
}
