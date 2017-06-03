package managers

import javax.inject._

import models.{AbstractModel, AbstractTable}
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import DBWrapper._
import DBWrapper.api._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Abstract manager to handle a model.
  *
  * This offers basic operations on models and an abstraction over the database.
  *
  * @param ec execution context in which to execute
  * @tparam Model type to manage
  * @tparam Table type of the table to manage
  */
abstract class AbstractManager[Model <: AbstractModel, Table <: AbstractTable[Model]](implicit ec: ExecutionContext) {
  /**
    * Application provider, to get access to the database configuration.
    */
  protected val appProvider: Provider[Application]

  /**
    * Query builder to use in the manager.
    */
  protected val query: TableQuery[Table]

  /**
    * Database on which to execute requests.
    */
  protected lazy val db: Backend#DatabaseDef = DatabaseConfigProvider.get[Profile](appProvider.get()).db

  /**
    * Copy the object and set its id to the one provided its id.
    *
    * @param t object to copy
    * @param id to give to the object
    * @return copy of the provided object with the new id
    */
  protected def withUpdatedId(t: Model, id: Long): Model

  /**
    * Insert the given object in the database and returns it with its new id.
    *
    * Please note that it doesn't fetch the object in database again, so if the database makes modifications
    * on it, these would get out of date.
    *
    * @param t object to save to the database
    * @return the object inserted with its new id
    */
  def create(t: Model): Future[Model] = {
    db.run(query.returning(query.map(_.id)).into[Model]((t, id) => withUpdatedId(t, id)) += t)
  }

  /**
    * Delete the object specified by the given id.
    *
    * @param id of the object to delete
    * @return number of objects deleted (0 or 1 in this case)
    */
  def delete(id: Long): Future[Int] = {
    db.run(query.filter(_.id === id).delete)
  }

  /**
    * Update the object identified by the given id to the value of the given object.
    *
    * @param id of the object to update
    * @param t the updated object
    * @return the updated object, without fetching it again from the database.
    */
  def update(id: Long, t: Model): Future[Option[Model]] = {
    db.run(query.filter(_.id === id).update(t).map {
      case 0 => None
      case _ => Some(t)
    })
  }
}
