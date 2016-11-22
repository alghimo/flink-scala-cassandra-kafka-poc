package org.alghimo.cassandra

import org.alghimo.models.AccountToAccountTransaction
import org.scalatest.AsyncFlatSpec

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountToAccountTransactionsDataSpec extends AsyncFlatSpec with WithAccountToAccountTransactionsData {
    override def accountToAccountTransactionsData: Seq[AccountToAccountTransaction] = Seq(
        AccountToAccountTransaction(
            srcAccount       = "FOOO1234",
            dstAccount       = "BAAR4321",
            sumAmounts    = 2.0,
            sumAmountsSqr = 4.0,
            numTransacs   = 1
        )
    )

    behavior of "WithAccountToAccountTransactionsData"

    it should "insert the data defined in the spec" in {
        val readStats = database.accountToAccountTransactions.getByAccounts("FOOO1234", "BAAR4321")

        readStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountToAccountTransaction should not be empty")
            val stats = maybeStats.get
            stats.srcAccount shouldEqual "FOOO1234"
            stats.dstAccount shouldEqual "BAAR4321"
            stats.sumAmounts shouldEqual 2.0
            stats.sumAmountsSqr shouldEqual 4.0
            stats.numTransacs shouldEqual 1
        }
    }
}
