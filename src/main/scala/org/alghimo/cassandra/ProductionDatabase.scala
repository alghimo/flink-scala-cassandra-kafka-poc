package org.alghimo.cassandra

/**
  * Created by alghimo on 11/13/2016.
  */
trait ProductionDatabase extends DataBaseProvider {
    override def database: FraudPocDatabase = FraudPocDatabase
}
