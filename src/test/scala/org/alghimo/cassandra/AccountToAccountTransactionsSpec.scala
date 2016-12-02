package org.alghimo.cassandra

import org.alghimo.models.AccountToAccountTransaction
import org.alghimo.utils.DatabaseTest
import org.scalatest.{AsyncFlatSpec, Inside}

/**
  * Created by alghimo on 11/15/2016.
  */
class AccountToAccountTransactionsSpec
    extends AsyncFlatSpec
    with DatabaseTest
    with TestDatabaseProvider
    with Inside
{
    behavior of "AccountStats"

    it should "eventually insert the new account to account transactions and retrieve the stats" in {
        val acc2AccStats = {
            val amounts = Seq(5.0, 10.0, 25.0)
            val sumAmounts = amounts.sum
            val sumAmountsSqr = amounts.map(amount => amount * amount).sum

            AccountToAccountTransaction(
                srcAccount    = "BE1234",
                dstAccount    = "NL9876",
                sumAmounts    = sumAmounts,
                sumAmountsSqr = sumAmountsSqr,
                numTransacs   = amounts.size
            )
        }

        val futureStats = for {
            store     <- database.accountToAccountTransactions.store(acc2AccStats)
            readStats <- database.accountToAccountTransactions.getByAccounts(acc2AccStats.srcAccount, acc2AccStats.dstAccount)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined
            val stats = maybeStats.get

            inside(stats) {
                case AccountToAccountTransaction(srcAccount, dstAccount, sumAmounts, sumAmountsSqr, numTransacs) =>
                    srcAccount    shouldEqual acc2AccStats.srcAccount
                    dstAccount    shouldEqual acc2AccStats.dstAccount
                    sumAmounts    shouldEqual acc2AccStats.sumAmounts
                    sumAmountsSqr shouldEqual acc2AccStats.sumAmountsSqr
                    numTransacs   shouldEqual acc2AccStats.numTransacs
            }
        }
    }

    it should "eventually update the account to account transactions and retrieve them" in {
        val srcAccount = "XX123"
        val dstAccount = "FR456"
        val originalAccStats = {
            val amounts       = Seq(5.0, 10.0, 25.0)
            val sumAmounts    = amounts.sum
            val sumAmountsSqr = amounts.map(amount => amount * amount).sum

            AccountToAccountTransaction(
                srcAccount    = srcAccount,
                dstAccount    = dstAccount,
                sumAmounts    = sumAmounts,
                sumAmountsSqr = sumAmountsSqr,
                numTransacs   = amounts.size
            )
        }

        val newAccStats = AccountToAccountTransaction(
            srcAccount    = srcAccount,
            dstAccount    = dstAccount,
            sumAmounts    = 27.0,
            sumAmountsSqr = 27.0 * 27.0,
            numTransacs   = 1
        )

        val resultingStats = AccountToAccountTransaction(
            srcAccount    = srcAccount,
            dstAccount    = dstAccount,
            sumAmounts    = originalAccStats.sumAmounts + newAccStats.sumAmounts,
            sumAmountsSqr = originalAccStats.sumAmountsSqr + newAccStats.sumAmountsSqr,
            numTransacs   = originalAccStats.numTransacs + newAccStats.numTransacs
        )
        val futureStats = for {
            inserted  <- database.accountToAccountTransactions.store(originalAccStats)
            updated   <- database.accountToAccountTransactions.store(newAccStats)
            readStats <- database.accountToAccountTransactions.getByAccounts(originalAccStats.srcAccount, originalAccStats.dstAccount)
        } yield readStats

        futureStats map { maybeStats =>
            maybeStats shouldBe defined
            val stats = maybeStats.get

            inside(stats) {
                case AccountToAccountTransaction(src, dst, sumAmounts, sumAmountsSqr, numTransacs) =>
                    src           shouldEqual resultingStats.srcAccount
                    dst           shouldEqual resultingStats.dstAccount
                    sumAmounts    shouldEqual resultingStats.sumAmounts
                    sumAmountsSqr shouldEqual resultingStats.sumAmountsSqr
                    numTransacs   shouldEqual resultingStats.numTransacs
            }
        }
    }
}
