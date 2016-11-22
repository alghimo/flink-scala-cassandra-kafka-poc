package org.alghimo.cassandra

import com.websudos.phantom.dsl
import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction}

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Created by alghimo on 11/18/2016.
  */
trait FraudPocDatabaseTest extends DatabaseTest {
    protected val insertTimeout = 30 seconds
    protected val dropTimeout   = 10 seconds
    protected val testTimeout   = 30 seconds
    def accountGlobalStatsData: Seq[AccountGlobalStats]
    def accountToAccountTransactionsData: Seq[AccountToAccountTransaction]
    def accountToCountryTransactionsData: Seq[AccountToCountryTransaction]


    override def setupFixtures(): Unit = {
        super.setupFixtures()
        import dsl.context

        val p = Promise[Future[Boolean]]()
        val accountGlobalStatsFutures = accountGlobalStatsData.map(database.accountStats.store(_))
        accountGlobalStatsFutures.foreach(_.onFailure{ case ex => p.tryFailure(ex)})

        val accountToAccountTransactionFutures = accountToAccountTransactionsData.map(database.accountToAccountTransactions.store(_))
        accountToAccountTransactionFutures.foreach(_.onFailure{ case ex => p.tryFailure(ex)})

        val accountToCountryTransactionFutures = accountToCountryTransactionsData.map(database.accountToCountryTransactions.store(_))
        accountToCountryTransactionFutures.foreach(_.onFailure{ case ex => p.tryFailure(ex)})

        val allFutures = accountGlobalStatsFutures ++ accountToAccountTransactionFutures ++ accountToCountryTransactionFutures
        Await.result(Future.sequence(allFutures), insertTimeout)
    }

    override def cleanupFixtures(): Unit = {
        import dsl.context
        Await.result(database.accountStats.truncate().future(), autoDropTimeout)
        Await.result(database.accountToAccountTransactions.truncate().future(), autoDropTimeout)
        Await.result(database.accountToCountryTransactions.truncate().future(), autoDropTimeout)
        super.cleanupFixtures()
    }
}
