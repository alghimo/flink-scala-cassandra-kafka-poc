package org.alghimo.cassandra

import org.alghimo.models.AccountToAccountTransaction
import org.scalatest.AsyncFlatSpec

/**
  * Created by alghimo on 11/15/2016.
  */
class AccountToAccountTransactionsSpec  extends AsyncFlatSpec with DatabaseTest {

    behavior of "AccountStats"

    it should "eventually insert the new account to account transactions and retrieve the stats" in {
        val amounts = Seq(5.0, 10.0, 25.0)
        val sumAmounts = amounts.sum
        val sumAmountsSqr = amounts.map(amount => amount * amount).sum
        val acc2AccStats = AccountToAccountTransaction(
            srcAccount       = "BE1234",
            dstAccount       = "NL9876",
            sumAmounts    = sumAmounts,
            sumAmountsSqr = sumAmountsSqr,
            numTransacs   = amounts.size
        )

        val futureStats = for {
            store <- database.accountToAccountTransactions.store(acc2AccStats)
            readStats <- database.accountToAccountTransactions.getByAccounts(acc2AccStats.srcAccount, acc2AccStats.dstAccount)
        } yield readStats

        futureStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountToAccountTransactions should not be empty")
            val stats = maybeStats.get
            stats.srcAccount shouldEqual acc2AccStats.srcAccount
            stats.dstAccount shouldEqual acc2AccStats.dstAccount
            stats.sumAmounts shouldEqual acc2AccStats.sumAmounts
            stats.sumAmountsSqr shouldEqual acc2AccStats.sumAmountsSqr
            stats.numTransacs shouldEqual acc2AccStats.numTransacs
        }
    }

    it should "eventually update the account to account transactions and retrieve them" in {
        val amounts = Seq(5.0, 10.0, 25.0)
        val sumAmounts = amounts.sum
        val sumAmountsSqr = amounts.map(amount => amount * amount).sum
        val srcAccount = "XX123"
        val dstAccount = "FR456"
        val originalAccStats = AccountToAccountTransaction(
            srcAccount       = srcAccount,
            dstAccount       = dstAccount,
            sumAmounts    = sumAmounts,
            sumAmountsSqr = sumAmountsSqr,
            numTransacs   = amounts.size
        )

        val newAccStats = AccountToAccountTransaction(
            srcAccount       = srcAccount,
            dstAccount       = dstAccount,
            sumAmounts    = 27.0,
            sumAmountsSqr = 27.0 * 27.0,
            numTransacs   = 1
        )

        val resultingStats = AccountToAccountTransaction(
            srcAccount       = srcAccount,
            dstAccount       = dstAccount,
            sumAmounts = originalAccStats.sumAmounts + newAccStats.sumAmounts,
            sumAmountsSqr = originalAccStats.sumAmountsSqr + newAccStats.sumAmountsSqr,
            numTransacs = originalAccStats.numTransacs + newAccStats.numTransacs
        )
        val futureStats = for {
            storeInsert <- database.accountToAccountTransactions.store(originalAccStats)
            storeUpdate <- database.accountToAccountTransactions.store(newAccStats)
            readStats <- database.accountToAccountTransactions.getByAccounts(originalAccStats.srcAccount, originalAccStats.dstAccount)
        } yield readStats

        futureStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountToAccountTransaction should not be empty")
            val stats = maybeStats.get
            stats.srcAccount shouldEqual resultingStats.srcAccount
            stats.dstAccount shouldEqual resultingStats.dstAccount
            stats.sumAmounts shouldEqual resultingStats.sumAmounts
            stats.sumAmountsSqr shouldEqual resultingStats.sumAmountsSqr
            stats.numTransacs shouldEqual resultingStats.numTransacs
        }
    }
}
