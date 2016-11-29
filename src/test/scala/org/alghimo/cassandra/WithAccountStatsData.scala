package org.alghimo.cassandra

import com.websudos.phantom.dsl
import org.alghimo.models.AccountGlobalStats

import scala.concurrent.{Await, Future, Promise}

/**
  * Created by alghimo on 11/18/2016.
  */
trait WithAccountStatsData extends DatabaseTest {
    import dsl.context
    def accountGlobalStatsData: Seq[AccountGlobalStats] = Seq()

    override def setupFixtures(): Unit = {
        super.setupFixtures()

        val p                         = Promise[Future[Boolean]]()
        val accountGlobalStatsFutures = accountGlobalStatsData.map(database.accountStats.store(_))
        accountGlobalStatsFutures
            .foreach(_.onFailure {
                case ex => p.tryFailure(ex)
            })

        Await.result(Future.sequence(accountGlobalStatsFutures), autoCreateTimeout)
    }

    override def cleanupFixtures(): Unit = {
        Await.result(database.accountStats.truncate().future(), autoDropTimeout)
        super.cleanupFixtures()
    }
}
