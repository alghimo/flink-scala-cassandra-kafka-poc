package org.alghimo.cassandra

/**
  * Created by alghimo on 11/13/2016.
  */
trait TestDatabaseProvider
    extends DatabaseProvider
    with TestDefaults.connector.Connector
{
    override def database: FraudPocDatabase = FraudPocTestDatabase
}
