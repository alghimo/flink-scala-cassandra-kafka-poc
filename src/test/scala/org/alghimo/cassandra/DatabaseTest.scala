package org.alghimo.cassandra

import com.websudos.phantom.dsl
import org.alghimo.BeforeAndAfterEachFixtures
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.duration._

trait DatabaseTest extends AsyncTestSuite
    with ScalaFutures
    with Matchers
    with OptionValues
    with BeforeAndAfterEachFixtures
    with TestDatabaseProvider
    with TestDefaults.connector.Connector
{
    protected val autoCreateTimeout = 5 seconds
    protected val autoDropTimeout   = 5 seconds

    override def setupFixtures(): Unit = {
        super.setupFixtures()
        import dsl.context
        // Automatically create every single table in Cassandra.
        Await.result(database.autocreate.future(), autoCreateTimeout)
    }

    override def cleanupFixtures(): Unit = {
        import dsl.context
        Await.result(database.autodrop().future(), autoDropTimeout)
        super.cleanupFixtures()
    }
}
