package org.alghimo.cassandra

/**
  * Created by alghimo on 11/13/2016.
  */
trait DatabaseProvider extends java.io.Serializable {
    def database: FraudPocDatabase
}
