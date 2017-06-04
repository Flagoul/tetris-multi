package managers

import javax.inject._

import managers.DBWrapper.api._
import models.{Result, ResultTable}
import play.api.Application

import scala.concurrent.ExecutionContext


@Singleton
class ResultManager @Inject()(val appProvider: Provider[Application])
                             (implicit ec: ExecutionContext) extends AbstractManager[Result, ResultTable] {

  protected val query: TableQuery[ResultTable] = TableQuery[ResultTable]

  override protected def withUpdatedId(result: Result, id: Long): Result = result.copy(id = Some(id))
}
