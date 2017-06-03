package models

import managers.DBWrapper.api._


/**
  * Base class to represent any model to be stored in the database.
  *
  * @param id unique identifier of the model
  */
abstract class AbstractModel(id: Option[Long])


/**
  * Represent any table that is stored in the database.
  *
  * @param tag to give to the table
  * @param name of the table
  * @tparam AbstractModel model stored in the table
  */
abstract class AbstractTable[AbstractModel](tag: Tag, name: String)
  extends Table[AbstractModel](tag, name) {

  /**
    * Id column in the table
    *
    * @return the id column
    */
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
}