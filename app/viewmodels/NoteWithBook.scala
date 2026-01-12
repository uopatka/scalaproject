package viewmodels

import models.{Note, Entry, Book}

case class NoteWithBook(
                         note: Note,
                         entry: Entry,
                         book: Book
                       )
