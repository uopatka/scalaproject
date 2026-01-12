package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import forms.{NoteForm, NoteData}
import models.{Note, Entry, Book}
import persistence.{NoteRepository, BookEntryRepository, BookRepository}
import java.time.LocalDateTime

@Singleton
class NoteController @Inject()(
                                cc: MessagesControllerComponents,
                                noteRepository: NoteRepository,
                                bookEntryRepository: BookEntryRepository,
                                bookRepository: BookRepository
                              )(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  //Adding notes
  def add(entryId: Long) = Action { implicit request =>
    Ok(views.html.createNote(NoteForm.form, entryId))
  }

  def addSubmit(entryId: Long) = Action.async { implicit request =>
    NoteForm.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.createNote(formWithErrors, entryId))),

      noteData => {
        val userId = request.session.get("userId").map(_.toLong).getOrElse(0L)

        val note = Note(
          id = 0L,
          entryId = entryId,
          userId = userId,
          title = noteData.title,
          content = noteData.content
        )

        noteRepository.create(note).map { _ =>
          Redirect(routes.HomeController.showBookByEntryId(entryId))
        }
      }
    )
  }

  //Editing notes
  def edit(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).map {
      case Some(note) =>
        Ok(
          views.html.editNote(
            NoteForm.form.fill(NoteData(note.title, note.content)),
            note
          )
        )
      case None => NotFound("Notatka nie istnieje")
    }
  }

  def editSubmit(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).flatMap {
      case Some(note) =>
        NoteForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(views.html.editNote(formWithErrors, note))),
          noteData => {
            val updatedNote = note.copy(
              title = noteData.title,
              content = noteData.content,
              updatedAt = LocalDateTime.now()
            )
            noteRepository.update(updatedNote).map { _ =>
              Redirect(routes.HomeController.showBookByEntryId(note.entryId))
            }
          }
        )
      case None => Future.successful(NotFound("Notatka nie istnieje"))
    }
  }

  //Deleting note
  def delete(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).flatMap {
      case Some(note) =>
        noteRepository.delete(noteId).map { _ =>
          Redirect(routes.HomeController.showBookByEntryId(note.entryId))
        }
      case None => Future.successful(NotFound("Notatka nie istnieje"))
    }
  }

  //showing list of notes
  def showNotes = Action.async { implicit request =>
    request.session.get("userId") match {
      case Some(id) =>
        val userId = id.toLong

        val notesF = noteRepository.findByUser(userId)

        val notesWithBooksF: Future[Seq[(Note, Entry, Book)]] =
          notesF.flatMap { notes =>
            Future.sequence {
              notes.map { note =>
                for {
                  entryOpt <- bookEntryRepository.getById(note.entryId)
                  bookOpt  <- entryOpt match {
                    case Some(entry) => bookRepository.getByIsbn(entry.refId)
                    case None        => Future.successful(None)
                  }
                } yield for {
                  entry <- entryOpt
                  book  <- bookOpt
                } yield (note, entry, book)
              }
            }.map(_.flatten)
          }

        val groupedF: Future[Map[Book, Seq[(Note, Entry)]]] = notesWithBooksF.map { notesWithBooks =>
          notesWithBooks
            .groupBy { case (_, _, book) => book }
            .map { case (book, seq) =>
              book -> seq.map { case (note, entry, _) => (note, entry) }
            }
        }

        groupedF.map { notesByBook =>
          Ok(views.html.showNoteGrouped(notesByBook))
        }

      case None =>
        Future.successful(Redirect(routes.AuthController.loginPage()))
    }
  }
}
