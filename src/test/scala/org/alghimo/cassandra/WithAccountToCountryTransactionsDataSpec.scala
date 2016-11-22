package org.alghimo.cassandra

import org.alghimo.models.AccountToCountryTransaction
import org.scalatest.AsyncFlatSpec

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountToCountryTransactionsDataSpec  extends AsyncFlatSpec with WithAccountToCountryTransactionsData {
    override def accountToCountryTransactionsData: Seq[AccountToCountryTransaction] = Seq(
        AccountToCountryTransaction(
            srcAccount    = "FOOO1234",
            dstCountry    = "FR",
            sumAmounts    = 2.0,
            sumAmountsSqr = 4.0,
            numTransacs   = 1
        )
    )

    behavior of "WithAccountToCountryTransactionsData"

    it should "insert the data defined in the spec" in {
        val readStats = database.accountToCountryTransactions.getByAccountAndCountry("FOOO1234", "FR")

        readStats map { maybeStats =>
            assert(maybeStats.isDefined, "AccountToCountryTransaction should not be empty")
            val stats = maybeStats.get
            stats.srcAccount shouldEqual "FOOO1234"
            stats.dstCountry shouldEqual "FR"
            stats.sumAmounts shouldEqual 2.0
            stats.sumAmountsSqr shouldEqual 4.0
            stats.numTransacs shouldEqual 1
        }
    }
}
