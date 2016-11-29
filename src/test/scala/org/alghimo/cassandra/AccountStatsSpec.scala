package org.alghimo.cassandra

import org.alghimo.models.AccountGlobalStats
import org.scalatest.{AsyncFlatSpec, Inside}
/**
  * Created by alghimo on 11/13/2016.
  */
class AccountStatsSpec
    extends AsyncFlatSpec
    with DatabaseTest
    with Inside
{
    behavior of "AccountStats"

    it should "eventually insert the new global stats for an account and retrieve them" in {
        val accStats = {
            val amounts       = Seq(5.0, 10.0, 25.0)
            val sumAmounts    = amounts.sum
            val sumAmountsSqr = amounts.map(amount => amount * amount).sum

            AccountGlobalStats(
                account       = "BE1234",
                sumAmounts    = sumAmounts,
                sumAmountsSqr = sumAmountsSqr,
                numTransacs   = amounts.size
            )
        }

        val futureStats = for {
            store     <- database.accountStats.store(accStats)
            readStats <- database.accountStats.getByAccount(accStats.account)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined

            val stats = maybeStats.get

            inside(stats) {
                case AccountGlobalStats(account, sumAmounts, sumAmountsSqr, numTransacs) =>
                    account       shouldEqual accStats.account
                    sumAmounts    shouldEqual accStats.sumAmounts
                    sumAmountsSqr shouldEqual accStats.sumAmountsSqr
                    numTransacs   shouldEqual accStats.numTransacs
            }
        }
    }

    it should "eventually update the global stats for an account and retrieve them" in {
        val originalAccStats = {
            val amounts       = Seq(5.0, 10.0, 25.0)
            val sumAmounts    = amounts.sum
            val sumAmountsSqr = amounts.map(amount => amount * amount).sum

            AccountGlobalStats(
                account       = "XX123",
                sumAmounts    = sumAmounts,
                sumAmountsSqr = sumAmountsSqr,
                numTransacs   = amounts.size
            )
        }

        val newAccStats = AccountGlobalStats(
            account       = "XX123",
            sumAmounts    = 27.0,
            sumAmountsSqr = 27.0 * 27.0,
            numTransacs   = 1
        )

        val resultingStats = AccountGlobalStats(
            account       = originalAccStats.account,
            sumAmounts    = originalAccStats.sumAmounts + newAccStats.sumAmounts,
            sumAmountsSqr = originalAccStats.sumAmountsSqr + newAccStats.sumAmountsSqr,
            numTransacs   = originalAccStats.numTransacs + newAccStats.numTransacs
        )
        val futureStats = for {
            inserted  <- database.accountStats.store(originalAccStats)
            updated   <- database.accountStats.store(newAccStats)
            readStats <- database.accountStats.getByAccount(originalAccStats.account)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined

            val stats = maybeStats.get

            inside(stats) {
                case AccountGlobalStats(account, sumAmounts, sumAmountsSqr, numTransacs) =>
                    account       shouldEqual resultingStats.account
                    sumAmounts    shouldEqual resultingStats.sumAmounts
                    sumAmountsSqr shouldEqual resultingStats.sumAmountsSqr
                    numTransacs   shouldEqual resultingStats.numTransacs
            }
        }
    }
}
