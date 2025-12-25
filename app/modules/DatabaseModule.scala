package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import javax.inject.Inject
import play.api.Configuration
import slick.jdbc.PostgresProfile.api.Database

class DatabaseModule extends AbstractModule {
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideSlickDatabase(conf: Configuration): Database =
    // If a jdbc URL is configured, use the configured database; otherwise fall back to an in-memory H2 DB for dev/tests
    conf.getOptional[String]("db.jdbcUrl") match {
      case Some(_) => Database.forConfig("db", conf.underlying)
      case None    => Database.forURL("jdbc:h2:mem:play;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
    }
}
