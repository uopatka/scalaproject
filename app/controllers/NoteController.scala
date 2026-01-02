package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import forms.{NoteForm, NoteData}  // <- konieczne importy
import models.Note
import persistence.NoteRepository
import java.time.LocalDateTime

@Singleton
class NoteController @Inject()(
                                cc: MessagesControllerComponents,
                                noteRepository: NoteRepository
                              )(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  def add(bookEntryId: Long) = Action { implicit request =>
    Ok(views.html.createNote(NoteForm.form, bookEntryId)) // <- NoteForm.form!
  }

  def addSubmit(bookEntryId: Long) = Action.async { implicit request =>
    NoteForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.createNote(formWithErrors, bookEntryId))),
      noteData => {
        val userId = request.session.get("userId").map(_.toLong).getOrElse(0L)
        val note = Note(0L, bookEntryId, userId, noteData.title, noteData.content)
        noteRepository.create(note).map(_ =>
          Redirect(routes.HomeController.showBookByEntryId(bookEntryId))
        )
      }
    )
  }

  def edit(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).map {
      case Some(note) =>
        Ok(views.html.editNote(NoteForm.form.fill(NoteData(note.title, note.content)), note))
      case None => NotFound("Notatka nie istnieje")
    }
  }

  def editSubmit(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).flatMap {
      case Some(note) =>
        NoteForm.form.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.html.editNote(formWithErrors, note))),
          noteData => {
            val updatedNote = note.copy(title = noteData.title, content = noteData.content, updatedAt = LocalDateTime.now())
            noteRepository.update(updatedNote).map { _ =>
              Redirect(routes.HomeController.showBookByEntryId(note.bookEntryId))
            }
          }
        )
      case None => Future.successful(NotFound("Notatka nie istnieje"))
    }
  }

  def delete(noteId: Long) = Action.async { implicit request =>
    noteRepository.findById(noteId).flatMap {
      case Some(note) => noteRepository.delete(noteId).map(_ =>
        Redirect(routes.HomeController.showBookByEntryId(note.bookEntryId))
      )
      case None => Future.successful(NotFound("Notatka nie istnieje"))
    }
  }
}
