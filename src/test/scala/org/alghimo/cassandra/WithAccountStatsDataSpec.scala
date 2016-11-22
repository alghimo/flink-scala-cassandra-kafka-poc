package org.alghimo.cassandra

import org.alghimo.models.AccountGlobalStats
import org.scalatest.AsyncFlatSpec

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountStatsDataSpec extends AsyncFlatSpec with WithAccountStatsData {
    override def accountGlobalStatsData: Seq[AccountGlobalStats] = Seq(
        AccountGlobalStats(
            account       = "FOOO1234",
            sumAmounts    = 2.0,
            sumAmountsSqr = 4.0,
            numTransacs   = 1
        )
    )

    behavior of "WithAccountStatsData"

    it should "insert the data defined in the spec" in {
        val readStats = database.accountStats.getByAccount("FOOO1234")

        readStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountStats should not be empty")
            val stats = maybeStats.get
            stats.account shouldEqual "FOOO1234"
            stats.sumAmounts shouldEqual 2.0
            stats.sumAmountsSqr shouldEqual 4.0
            stats.numTransacs shouldEqual 1
        }
    }
}
