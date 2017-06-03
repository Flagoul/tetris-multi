package managers

import slick.jdbc.{JdbcProfile, MySQLProfile}

/**
  * This is a wrapper around the database profile used, in order not to get too dependent on a specific database.

  */
object DBWrapper extends MySQLProfile {
  /**
    * Profile of the database to use.
    */
  type Profile = JdbcProfile
}
