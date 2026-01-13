package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  override def fakeApplication() = {
    val mockBookRepo = mock[persistence.BookRepository]
    val mockPublicationRepo = mock[persistence.PublicationRepository]
    val mockEntryRepo = mock[persistence.BookEntryRepository]
    val mockUserRepo = mock[persistence.UserRepository]
    val mockNoteRepo = mock[persistence.NoteRepository]
    val mockOpenLib = mock[services.OpenLibraryService]
    val mockCrossref = mock[services.CrossrefService]

    when(mockOpenLib.fetchByIsbn(ArgumentMatchers.anyString())).thenReturn(Future.successful(None))
    when(mockCrossref.fetchByDoi(ArgumentMatchers.anyString())).thenReturn(Future.successful(None))
    when(mockBookRepo.getAll()).thenReturn(Future.successful(Seq.empty))
    when(mockPublicationRepo.getAll()).thenReturn(Future.successful(Seq.empty))
    when(mockEntryRepo.getAll()).thenReturn(Future.successful(Seq.empty))

    GuiceApplicationBuilder()
      .overrides(
        bind[persistence.BookRepository].toInstance(mockBookRepo),
        bind[persistence.PublicationRepository].toInstance(mockPublicationRepo),
        bind[persistence.BookEntryRepository].toInstance(mockEntryRepo),
        bind[persistence.UserRepository].toInstance(mockUserRepo),
        bind[persistence.NoteRepository].toInstance(mockNoteRepo),
        bind[services.OpenLibraryService].toInstance(mockOpenLib),
        bind[services.CrossrefService].toInstance(mockCrossref)
      ).build()
  }

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val controller = inject[HomeController]
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Bugshelv")
    }

    "render the index page from the application" in {
      val controller = inject[HomeController]
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Bugshelv")
    }

    "render the index page from the router" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Bugshelv")
    }
  }
}
