package org.alghimo.cassandra

import com.websudos.phantom.dsl._

/**
  * Created by alghimo on 9/12/2016.
  */
class FraudPocDatabase(val keyspace: KeySpaceDef) extends Database[FraudPocDatabase](keyspace) {
    object accountToAccountTransactions extends ConcreteAccountToAccountTransactions with keyspace.Connector
    object accountToCountryTransactions extends ConcreteAccountToCountryTransactions with keyspace.Connector
    object accountStats                 extends ConcreteAccountStats                 with keyspace.Connector
}

object FraudPocDatabase extends FraudPocDatabase(Defaults.connector)