package org.alghimo.cassandra

import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction}
import org.scalatest.{AsyncFlatSpec, Inside, Matchers}
/**
  * Created by alghimo on 11/18/2016.
  */
class FraudPocDatabaseTestSpec
    extends AsyncFlatSpec
    with Matchers
    with Inside
    with WithAccountStatsData
    with WithAccountToAccountTransactionsData
    with WithAccountToCountryTransactionsData
{
    override def accountGlobalStatsData: Seq[AccountGlobalStats] = Seq(
        AccountGlobalStats("FOO1234", 2.0, 4.0, 1)
    )

    override def accountToAccountTransactionsData = Seq(
        AccountToAccountTransaction("FOO1234", "BAR4321", 2.0, 4.0, 1)
    )

    override def accountToCountryTransactionsData = Seq(
        AccountToCountryTransaction("FOO1234", "IT", 2.0, 4.0, 1)
    )

    val accountStatsToStore = AccountGlobalStats("ABC123", 2.0, 4.0, 1)

    behavior of "FraudPocDatabaseTest"

    for (attempt <- 1 to 5) {
        it should s"start with fresh data for account stats - attempt ${attempt}" in {
            val accountStats = accountGlobalStatsData.head
            val futureStats  = for {
                existingStats <- database.accountStats.getByAccount(accountStats.account)
                updated       <- database.accountStats.store(accountStats.copy(sumAmounts = 10.0, sumAmountsSqr = 100.0))
                updatedStats  <- database.accountStats.getByAccount(accountStats.account)
            } yield (existingStats, updatedStats)

            futureStats map { maybeStats =>
                maybeStats._1 shouldBe defined
                maybeStats._2 shouldBe defined

                inside(maybeStats._1.get) {
                    case AccountGlobalStats(acc, sumAmounts, sumAmountsSqr, numTransacs) =>
                        acc           shouldEqual accountStats.account
                        sumAmounts    shouldEqual accountStats.sumAmounts
                        sumAmountsSqr shouldEqual accountStats.sumAmountsSqr
                        numTransacs   shouldEqual accountStats.numTransacs
                }
                val expectedStats = AccountGlobalStats(
                    account       = accountStats.account,
                    sumAmounts    = accountStats.sumAmounts + 10.0,
                    sumAmountsSqr = accountStats.sumAmountsSqr + 100.0,
                    numTransacs   = accountStats.numTransacs + 1
                )

                inside(maybeStats._2.get) {
                    case AccountGlobalStats(acc, sumAmounts, sumAmountsSqr, numTransacs) =>
                        acc           shouldEqual expectedStats.account
                        sumAmounts    shouldEqual expectedStats.sumAmounts
                        sumAmountsSqr shouldEqual expectedStats.sumAmountsSqr
                        numTransacs   shouldEqual expectedStats.numTransacs
                }
            }
        }
    }
}
