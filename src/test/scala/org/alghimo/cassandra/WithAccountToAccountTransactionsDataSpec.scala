package org.alghimo.cassandra

import org.alghimo.models.AccountToAccountTransaction
import org.scalatest.{AsyncFlatSpec, Inside}

/**
  * Created by alghimo on 11/18/2016.
  */
class WithAccountToAccountTransactionsDataSpec
    extends AsyncFlatSpec
    with WithAccountToAccountTransactionsData
    with Inside
{
    override def accountToAccountTransactionsData: Seq[AccountToAccountTransaction] = Seq(
        AccountToAccountTransaction(
            srcAccount    = "FOOO1234",
            dstAccount    = "BAAR4321",
            sumAmounts    = 2.0,
            sumAmountsSqr = 4.0,
            numTransacs   = 1
        )
    )

    behavior of "WithAccountToAccountTransactionsData"

    it should "insert the data defined in the spec" in {
        val expectedTransaction = accountToAccountTransactionsData.head
        val readStats           = database.accountToAccountTransactions.getByAccounts(expectedTransaction.srcAccount, expectedTransaction.dstAccount)

        readStats map { maybeStats =>
            maybeStats shouldBe defined

            val stats = maybeStats.get

            inside(stats) {
                case AccountToAccountTransaction(srcAccount, dstAccount, sumAmounts, sumAmountsSqr, numTransacs) =>
                    srcAccount    shouldEqual expectedTransaction.srcAccount
                    dstAccount    shouldEqual expectedTransaction.dstAccount
                    sumAmounts    shouldEqual expectedTransaction.sumAmounts
                    sumAmountsSqr shouldEqual expectedTransaction.sumAmountsSqr
                    numTransacs   shouldEqual expectedTransaction.numTransacs
            }
        }
    }
}
