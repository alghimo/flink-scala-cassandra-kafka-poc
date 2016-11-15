package org.alghimo.cassandra

import com.websudos.phantom.dsl
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DatabaseTest extends Suite
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers
    with OptionValues
    with TestDatabaseProvider
    with TestDefaults.connector.Connector {
    override def beforeAll(): Unit = {
        import dsl.context
        super.beforeAll()
        // Automatically create every single table in Cassandra.
        Await.result(database.autocreate.future(), 5.seconds)
    }
}
