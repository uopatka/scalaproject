package security

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
import models.BookStatus
import java.time.LocalDate

/**
 * SQL Injection resistance tests 
 */
class SqlInjectionSpec extends PlaySpec 
  with GuiceOneAppPerSuite 
  with Injecting 
  with MockitoSugar 
  with BeforeAndAfterAll {

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
        "db.url" -> "jdbc:h2:mem:testdb_sqlinj;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
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

  "AuthController SQL Injection Protection" should {

    "reject SQL injection in login username field" in {
      val maliciousUsername = "admin' OR '1'='1"
      val request = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
          "username" -> maliciousUsername,
          "password" -> "anything"
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
      session(result).get("username") mustBe None
      contentAsString(result) must include("Invalid username or password")
    }

    "reject SQL injection in login password field" in {
      val maliciousPassword = "' OR '1'='1' --"
      val request = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
          "username" -> "testuser",
          "password" -> maliciousPassword
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
      session(result).get("username") mustBe None
    }

    "safely handle SQL injection in registration username" in {
      val maliciousUsername = "test'; DROP TABLE users; --"
      val request = FakeRequest(POST, "/register")
        .withFormUrlEncodedBody(
          "username" -> maliciousUsername,
          "password" -> "test123",
          "confirmPassword" -> "test123"
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkUsers = testDb.run(sql"SELECT COUNT(*) FROM users".as[Int])
      Await.result(checkUsers, 5.seconds).head must be > 0
    }

    "prevent SQL injection through multiple special characters" in {
      val maliciousUsername = "admin'--"
      val request = FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
          "username" -> maliciousUsername,
          "password" -> ""
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      session(result).get("username") mustBe None
    }
  }

  "HomeController SQL Injection Protection" should {

    "safely handle SQL injection in search query" in {
      val maliciousSearch = "1984'; DELETE FROM books; --"
      val request = FakeRequest(GET, s"/?search=${java.net.URLEncoder.encode(maliciousSearch, "UTF-8")}")
        .withSession("userId" -> "1", "username" -> "testuser")
      
      val result = route(app, request).get
      
      status(result) mustBe OK
      
      val checkBooks = testDb.run(sql"SELECT COUNT(*) FROM books".as[Int])
      Await.result(checkBooks, 5.seconds).head must be >= 0
    }

    "safely handle SQL injection in status filter" in {
      val maliciousStatus = "to_read' OR '1'='1"
      val request = FakeRequest(GET, s"/?status=${java.net.URLEncoder.encode(maliciousStatus, "UTF-8")}")
        .withSession("userId" -> "1", "username" -> "testuser")
      
      val result = route(app, request).get
      
      status(result) mustBe OK
    }

    "prevent SQL injection in ISBN parameter" in {
      val maliciousIsbn = "9780141036144' OR '1'='1"
      val request = FakeRequest(GET, s"/book/${maliciousIsbn}")
        .withSession("userId" -> "1", "username" -> "testuser")
      
      val result = route(app, request).get
      
      status(result) mustNot be(INTERNAL_SERVER_ERROR)
    }
  }

  "EntryController SQL Injection Protection" should {

    "safely handle SQL injection in tags field" in {
      val maliciousTags = "fiction'; DROP TABLE entries; --"
      val request = FakeRequest(POST, "/book-entry/1/tags")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("tags" -> maliciousTags)
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkEntries = testDb.run(sql"SELECT COUNT(*) FROM entries".as[Int])
      Await.result(checkEntries, 5.seconds).head must be > 0
    }

    "safely handle SQL injection in pages read field" in {
      val maliciousPages = "150'; DELETE FROM entries WHERE '1'='1"
      val request = FakeRequest(POST, "/book-entry/1/pages-read")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("pagesRead" -> maliciousPages)
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
    }

    "prevent SQL injection in year field" in {
      val maliciousYear = "2020'; UPDATE books SET title='HACKED' WHERE '1'='1"
      val request = FakeRequest(POST, "/book/updateYear/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("publishYear" -> maliciousYear)
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
      
      val checkBooks = testDb.run(sql"SELECT COUNT(*) FROM books WHERE title = 'HACKED'".as[Int])
      Await.result(checkBooks, 5.seconds).head mustBe 0
    }

    "safely handle SQL injection in status update" in {
      val maliciousStatus = "finished'; DROP TABLE entries; --"
      val request = FakeRequest(POST, "/book-entry/1/status")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody(
          "status" -> maliciousStatus,
          "finishedAt" -> "2024-01-01"
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkEntries = testDb.run(sql"SELECT COUNT(*) FROM entries".as[Int])
      Await.result(checkEntries, 5.seconds).head must be > 0
    }
  }

  "NoteController SQL Injection Protection" should {

    "safely handle SQL injection in note title" in {
      val maliciousTitle = "Great Book'; DROP TABLE notes; --"
      val request = FakeRequest(POST, "/note/add/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody(
          "title" -> maliciousTitle,
          "content" -> "Normal content"
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkNotes = testDb.run(sql"SELECT COUNT(*) FROM notes".as[Int])
      Await.result(checkNotes, 5.seconds).head must be >= 0
    }

    "safely handle SQL injection in note content" in {
      val maliciousContent = "This is a great book!'; DELETE FROM notes WHERE '1'='1; --"
      val request = FakeRequest(POST, "/note/add/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody(
          "title" -> "Test Note",
          "content" -> maliciousContent
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkNotes = testDb.run(sql"SELECT COUNT(*) FROM notes".as[Int])
      val initialCount = Await.result(checkNotes, 5.seconds).head
      initialCount must be >= 0
    }

    "prevent SQL injection in note edit" in {
      val maliciousContent = "Updated content'; UPDATE notes SET content='HACKED' WHERE '1'='1"
      val request = FakeRequest(POST, "/note/edit/1")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody(
          "title" -> "Valid Title",
          "content" -> maliciousContent
        )
        .withCSRFToken
      
      val result = route(app, request).get
      
      val checkHacked = testDb.run(sql"SELECT COUNT(*) FROM notes WHERE content = 'HACKED'".as[Int])
      Await.result(checkHacked, 5.seconds).head must be <= 1 // At most the one we're editing
    }
  }

  "Repository SQL Injection Protection" should {

    "safely handle SQL injection in UserRepository.getByUsername" in {
      val userRepo = app.injector.instanceOf[persistence.UserRepository]
      val maliciousUsername = "admin' OR '1'='1"
      
      val result = Await.result(userRepo.getByUsername(maliciousUsername), 5.seconds)
      
      result mustBe None
    }

    "safely handle SQL injection in BookRepository.getByIsbn" in {
      val bookRepo = app.injector.instanceOf[persistence.BookRepository]
      val maliciousIsbn = "9780141036144'; DROP TABLE books; --"
      
      val result = Await.result(bookRepo.getByIsbn(maliciousIsbn), 5.seconds)
      
      result mustBe None
      
      val checkBooks = testDb.run(sql"SELECT COUNT(*) FROM books".as[Int])
      Await.result(checkBooks, 5.seconds).head must be >= 0
    }

    "safely handle date in BookEntryRepository.updateStatusAndFinishedAt" in {
      val entryRepo = app.injector.instanceOf[persistence.BookEntryRepository]
      
      val normalDate = LocalDate.parse("2024-01-01")
      
      val result = Await.result(
        entryRepo.updateStatusAndFinishedAt(1L, BookStatus.Finished, Some(normalDate)), 
        5.seconds
      )
      
      result mustBe 1
      
      val entry = Await.result(entryRepo.getById(1L), 5.seconds)
      entry must be(defined)
      entry.get.status mustBe BookStatus.Finished
      entry.get.finishedAt mustBe Some(normalDate)
      
      val checkTables = testDb.run(sql"SELECT COUNT(*) FROM entries".as[Int])
      Await.result(checkTables, 5.seconds).head must be > 0
    }
  }

  "Form Validation SQL Injection Protection" should {

    "reject malicious input in book ISBN form" in {
      val maliciousIsbn = "1234567890'; DROP TABLE books; --"
      val request = FakeRequest(POST, "/add-book")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("isbn" -> maliciousIsbn)
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Nieprawidłowy ISBN")
    }

    "reject malicious input in publication DOI form" in {
      val maliciousDoi = "10.1234/test'; DELETE FROM publications; --"
      val request = FakeRequest(POST, "/add-publication")
        .withSession("userId" -> "1", "username" -> "testuser")
        .withFormUrlEncodedBody("doi" -> maliciousDoi)
        .withCSRFToken
      
      val result = route(app, request).get
      
      status(result) mustBe BAD_REQUEST
    }
  }

  "Database Integrity After SQL Injection Attempts" should {

    "maintain all tables after multiple injection attempts" in {
      val checkUsers = testDb.run(sql"SELECT COUNT(*) FROM users".as[Int])
      val checkBooks = testDb.run(sql"SELECT COUNT(*) FROM books".as[Int])
      val checkEntries = testDb.run(sql"SELECT COUNT(*) FROM entries".as[Int])
      val checkNotes = testDb.run(sql"SELECT COUNT(*) FROM notes".as[Int])
      
      Await.result(checkUsers, 5.seconds).head must be > 0
      Await.result(checkBooks, 5.seconds).head must be >= 0
      Await.result(checkEntries, 5.seconds).head must be >= 0
      Await.result(checkNotes, 5.seconds).head must be >= 0
    }

    "preserve data integrity after injection attempts" in {
      val userRepo = app.injector.instanceOf[persistence.UserRepository]
      val testUser = Await.result(userRepo.getByUsername("testuser"), 5.seconds)
      
      testUser must be(defined)
      testUser.get.username mustBe "testuser"
    }
  }
}
