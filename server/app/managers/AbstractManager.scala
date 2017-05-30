package managers

import javax.inject._

import models.{AbstractModel, AbstractTable}
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.{JdbcBackend, MySQLProfile}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}


abstract class AbstractManager[T <: AbstractModel, TableDef <: AbstractTable[T]](implicit ec: ExecutionContext) {
  protected val appProvider: Provider[Application]
  protected val query: TableQuery[TableDef]

  protected lazy val db: JdbcBackend#DatabaseDef = DatabaseConfigProvider.get[MySQLProfile](appProvider.get()).db

  protected def withUpdatedId(t: T, id: Long): T

  def create(t: T): Future[T] = {
    db.run(query.returning(query.map(_.id)).into[T]((t, id) => withUpdatedId(t, id)) += t)
  }

  def delete(id: Long): Future[Int] = {
    db.run(query.filter(_.id === id).delete)
  }

  def get(id: Long): Future[Option[T]] = {
    db.run(query.filter(_.id === id).result.headOption)
  }

  def update(id: Long, t: T): Future[Option[T]] = {
    db.run(query.filter(_.id === id).update(t).map {
      case 0 => None
      case _ => Some(t)
    })
  }
}
