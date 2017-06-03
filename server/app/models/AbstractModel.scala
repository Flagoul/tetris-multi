package models

import managers.DBWrapper.api._


abstract class AbstractModel(id: Option[Long])


abstract class AbstractTable[AbstractModel](_tableTag: Tag, _tableName: String)
  extends Table[AbstractModel](_tableTag, _tableName) {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
}