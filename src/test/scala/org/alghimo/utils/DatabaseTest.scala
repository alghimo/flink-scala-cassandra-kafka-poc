package org.alghimo.utils

import com.websudos.phantom.dsl
import org.alghimo.cassandra.{DatabaseProvider, TestDefaults}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.duration._

trait DatabaseTest extends AsyncTestSuite
    with ScalaFutures
    with Matchers
    with OptionValues
    with BeforeAndAfterEachFixtures
    with DatabaseProvider
    with TestDefaults.connector.Connector
{
    import dsl.context
    protected lazy val autoCreateTimeout = 5 seconds
    protected lazy val autoDropTimeout   = 5 seconds

    override def setupFixtures(): Unit = {
        super.setupFixtures()

        // Automatically create every single table in Cassandra.
        Await.result(database.autocreate.future(), autoCreateTimeout)
    }

    override def cleanupFixtures(): Unit = {
        Await.result(database.autodrop().future(), autoDropTimeout)
        super.cleanupFixtures()
    }
}
