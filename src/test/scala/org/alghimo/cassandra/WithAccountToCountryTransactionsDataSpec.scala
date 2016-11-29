package org.alghimo.cassandra

import org.alghimo.models.AccountToCountryTransaction
import org.scalatest.{AsyncFlatSpec, Inside}

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountToCountryTransactionsDataSpec
    extends AsyncFlatSpec
    with WithAccountToCountryTransactionsData
    with Inside
{
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
            maybeStats shouldBe defined

            val stats         = maybeStats.get
            val expectedStats = accountToCountryTransactionsData.head

            inside(stats) {
                case AccountToCountryTransaction(srcAccount, dstCountry, sumAmounts, sumAmountsSqr, numTransacs) =>
                    srcAccount    shouldEqual expectedStats.srcAccount
                    dstCountry    shouldEqual expectedStats.dstCountry
                    sumAmounts    shouldEqual expectedStats.sumAmounts
                    sumAmountsSqr shouldEqual expectedStats.sumAmountsSqr
                    numTransacs   shouldEqual expectedStats.numTransacs
            }
        }
    }
}
