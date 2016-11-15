package org.alghimo.cassandra

import org.alghimo.models.AccountGlobalStats
import org.scalatest.AsyncFlatSpec
/**
  * Created by alghimo on 11/13/2016.
  */
class AccountStatsSpec extends AsyncFlatSpec with DatabaseTest {

    behavior of "AccountStats"

    it should "eventually insert the new global stats for an account and retrieve them" in {
        val amounts = Seq(5.0, 10.0, 25.0)
        val sumAmounts = amounts.sum
        val sumAmountsSqr = amounts.map(amount => amount * amount).sum
        val accStats = AccountGlobalStats(
            account       = "BE1234",
            sumAmounts    = sumAmounts,
            sumAmountsSqr = sumAmountsSqr,
            numTransacs   = amounts.size
        )

        val futureStats = for {
            store <- database.accountStats.store(accStats)
            readStats <- database.accountStats.getByAccount(accStats.account)
        } yield readStats

        futureStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountStats should not be empty")
            val stats = maybeStats.get
            stats.account shouldEqual accStats.account
            stats.sumAmounts shouldEqual accStats.sumAmounts
            stats.sumAmountsSqr shouldEqual accStats.sumAmountsSqr
            stats.numTransacs shouldEqual accStats.numTransacs
        }
    }

    it should "eventually update the global stats for an account and retrieve them" in {
        val amounts = Seq(5.0, 10.0, 25.0)
        val sumAmounts = amounts.sum
        val sumAmountsSqr = amounts.map(amount => amount * amount).sum
        val originalAccStats = AccountGlobalStats(
            account       = "XX123",
            sumAmounts    = sumAmounts,
            sumAmountsSqr = sumAmountsSqr,
            numTransacs   = amounts.size
        )

        val newAccStats = AccountGlobalStats(
            account       = "XX123",
            sumAmounts    = 27.0,
            sumAmountsSqr = 27.0 * 27.0,
            numTransacs   = 1
        )

        val resultingStats = AccountGlobalStats(
            account = originalAccStats.account,
            sumAmounts = originalAccStats.sumAmounts + newAccStats.sumAmounts,
            sumAmountsSqr = originalAccStats.sumAmountsSqr + newAccStats.sumAmountsSqr,
            numTransacs = originalAccStats.numTransacs + newAccStats.numTransacs
        )
        val futureStats = for {
            storeInsert <- database.accountStats.store(originalAccStats)
            storeUpdate <- database.accountStats.store(newAccStats)
            readStats <- database.accountStats.getByAccount(originalAccStats.account)
        } yield readStats

        futureStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountStats should not be empty")
            val stats = maybeStats.get
            stats.account shouldEqual resultingStats.account
            stats.sumAmounts shouldEqual resultingStats.sumAmounts
            stats.sumAmountsSqr shouldEqual resultingStats.sumAmountsSqr
            stats.numTransacs shouldEqual resultingStats.numTransacs
        }
    }
}
