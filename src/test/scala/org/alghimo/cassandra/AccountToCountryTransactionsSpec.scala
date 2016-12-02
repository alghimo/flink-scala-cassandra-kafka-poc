package org.alghimo.cassandra

import org.alghimo.models.AccountToCountryTransaction
import org.alghimo.utils.DatabaseTest
import org.scalatest.{AsyncFlatSpec, Inside}

/**
  * Created by alghimo on 11/15/2016.
  */
class AccountToCountryTransactionsSpec
    extends AsyncFlatSpec
    with DatabaseTest
    with TestDatabaseProvider
    with Inside
{
    behavior of "AccountStats"

    it should "eventually insert the new account to country transactions and retrieve the stats" in {
        val acc2CountryStats = {
            val amounts       = Seq(5.0, 10.0, 25.0)
            val sumAmounts    = amounts.sum
            val sumAmountsSqr = amounts.map(amount => amount * amount).sum

            AccountToCountryTransaction(
                srcAccount    = "BE1234",
                dstCountry    = "NL",
                sumAmounts    = sumAmounts,
                sumAmountsSqr = sumAmountsSqr,
                numTransacs   = amounts.size
            )
        }

        val futureStats = for {
            stored    <- database.accountToCountryTransactions.store(acc2CountryStats)
            readStats <- database.accountToCountryTransactions.getByAccountAndCountry(acc2CountryStats.srcAccount, acc2CountryStats.dstCountry)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined

            val stats = maybeStats.get

            stats.srcAccount shouldEqual acc2CountryStats.srcAccount
            stats.dstCountry shouldEqual acc2CountryStats.dstCountry
            stats.sumAmounts shouldEqual acc2CountryStats.sumAmounts
            stats.sumAmountsSqr shouldEqual acc2CountryStats.sumAmountsSqr
            stats.numTransacs shouldEqual acc2CountryStats.numTransacs
        }
    }

    it should "eventually update the account to country transactions and retrieve them" in {
        val amounts = Seq(5.0, 10.0, 25.0)
        val sumAmounts = amounts.sum
        val sumAmountsSqr = amounts.map(amount => amount * amount).sum
        val srcAccount = "XX123"
        val dstCountry = "IT"
        val originalStats = AccountToCountryTransaction(
            srcAccount    = srcAccount,
            dstCountry    = dstCountry,
            sumAmounts    = sumAmounts,
            sumAmountsSqr = sumAmountsSqr,
            numTransacs   = amounts.size
        )

        val newStats = AccountToCountryTransaction(
            srcAccount    = srcAccount,
            dstCountry    = dstCountry,
            sumAmounts    = 27.0,
            sumAmountsSqr = 27.0 * 27.0,
            numTransacs   = 1
        )

        val resultingStats = AccountToCountryTransaction(
            srcAccount    = srcAccount,
            dstCountry    = dstCountry,
            sumAmounts    = originalStats.sumAmounts + newStats.sumAmounts,
            sumAmountsSqr = originalStats.sumAmountsSqr + newStats.sumAmountsSqr,
            numTransacs   = originalStats.numTransacs + newStats.numTransacs
        )
        val futureStats = for {
            storeInsert <- database.accountToCountryTransactions.store(originalStats)
            storeUpdate <- database.accountToCountryTransactions.store(newStats)
            readStats <- database.accountToCountryTransactions.getByAccountAndCountry(originalStats.srcAccount, originalStats.dstCountry)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined
            val stats = maybeStats.get

            inside(stats) {
                case AccountToCountryTransaction(src, dst, sumAmt, sumAmtSqr, numTrans) =>
                    src       shouldEqual resultingStats.srcAccount
                    dst       shouldEqual resultingStats.dstCountry
                    sumAmt    shouldEqual resultingStats.sumAmounts
                    sumAmtSqr shouldEqual resultingStats.sumAmountsSqr
                    numTrans  shouldEqual resultingStats.numTransacs
            }
        }
    }
}
