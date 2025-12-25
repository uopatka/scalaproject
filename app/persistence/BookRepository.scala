package persistence

import models.Book
import persistence.tables.Books
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import javax.inject._

@Singleton
class BookRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {
  private val table = TableQuery[Books]

  def insert(book: Book): Future[String] =
    db.run((table returning table.map(_.isbn)) += book)

  def getByIsbn(isbn: String): Future[Option[Book]] =
    db.run(table.filter(_.isbn === isbn).result.headOption)

  def getAll(): Future[Seq[Book]] =
    db.run(table.result)

  def update(book: Book): Future[Int] =
    db.run(table.filter(_.isbn === book.isbn).update(book))

  def delete(isbn: String): Future[Int] =
    db.run(table.filter(_.isbn === isbn).delete)

  def upsert(book: Book): Future[Int] =
    db.run(table.insertOrUpdate(book))
}

