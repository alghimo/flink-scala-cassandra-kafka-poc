package org.alghimo.cassandra

import com.websudos.phantom.dsl
import org.alghimo.models.AccountToCountryTransaction

import scala.concurrent.{Await, Future, Promise}
/**
  * Created by alghimo on 11/18/2016.
  */
trait WithAccountToCountryTransactionsData extends DatabaseTest {
    import dsl.context
    def accountToCountryTransactionsData: Seq[AccountToCountryTransaction] = Seq()

    override def setupFixtures(): Unit = {
        super.setupFixtures()

        val p                                  = Promise[Future[Boolean]]()
        val accountToCountryTransactionFutures = accountToCountryTransactionsData
            .map(database.accountToCountryTransactions.store(_))
        accountToCountryTransactionFutures
            .foreach(_.onFailure {
                case ex => p.tryFailure(ex)
            })

        Await.result(Future.sequence(accountToCountryTransactionFutures), autoCreateTimeout)
    }

    override def cleanupFixtures(): Unit = {
        Await.result(database.accountToCountryTransactions.truncate().future(), autoDropTimeout)
        super.cleanupFixtures()
    }
}
