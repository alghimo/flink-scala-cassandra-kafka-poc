package org.alghimo.cassandra

import org.alghimo.models.AccountGlobalStats
import org.scalatest.{AsyncFlatSpec, Inside}

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountStatsDataSpec
    extends AsyncFlatSpec
    with WithAccountStatsData
    with Inside
{
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
            maybeStats shouldBe defined

            val stats         = maybeStats.get
            val expectedStats = accountGlobalStatsData.head

            inside(stats) {
                case AccountGlobalStats(account, sumAmounts, sumAmountsSqr, numTransacs)  =>
                    account       shouldEqual expectedStats.account
                    sumAmounts    shouldEqual expectedStats.sumAmounts
                    sumAmountsSqr shouldEqual expectedStats.sumAmountsSqr
                    numTransacs   shouldEqual expectedStats.numTransacs
            }
        }
    }
}
