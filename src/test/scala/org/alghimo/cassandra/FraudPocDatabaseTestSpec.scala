package org.alghimo.cassandra

import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction}
import org.scalatest.AsyncFlatSpec
/**
  * Created by alghimo on 11/18/2016.
  */
class FraudPocDatabaseTestSpec
    extends AsyncFlatSpec
    with WithAccountStatsData
    with WithAccountToAccountTransactionsData
    with WithAccountToCountryTransactionsData
{
    case class TestFutureResults(
        existingStats: Option[AccountGlobalStats],
        updatedStats:  Option[AccountGlobalStats],
        emptyStats:    Option[AccountGlobalStats],
        newStats:      Option[AccountGlobalStats]
    )

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

    def assertAccountStats(actual: AccountGlobalStats, expected: AccountGlobalStats) = {
        println("actual.account == expected.account : " + (actual.account == expected.account))
        assert(actual.account == expected.account, s"Account ${actual.account} and ${expected.account} should be equal")
        println(s"actual.sumAmounts (${actual.sumAmounts}) == expected.sumAmounts (${expected.sumAmounts}): " + (actual.sumAmounts == expected.sumAmounts))
        assert(actual.sumAmounts == expected.sumAmounts, s"sumAmounts ${actual.sumAmounts} and ${expected.sumAmounts} should be equal")
        println("actual.sumAmountsSqr == expected.sumAmountsSqr : " + (actual.sumAmountsSqr == expected.sumAmountsSqr))
        assert(actual.sumAmountsSqr == expected.sumAmountsSqr, s"sumAmountsSqr ${actual.sumAmountsSqr} and ${expected.sumAmountsSqr} should be equal")
        println("actual.numTransacs   == expected.numTransacs : " + (actual.numTransacs   == expected.numTransacs))
        assert(actual.numTransacs   == expected.numTransacs, s"numTransacs ${actual.numTransacs} and ${expected.numTransacs} should be equal")
    }

    behavior of "FraudPocDatabaseTest"

    for (attempt <- 1 to 5) {
        it should s"start with fresh data for account stats - attempt ${attempt}" in {
            val futureStats = for {
                existingStats <- database.accountStats.getByAccount(accountGlobalStatsData.head.account)
                updated <- database.accountStats.store(accountGlobalStatsData.head.copy(sumAmounts = 10.0, sumAmountsSqr = 100.0))
                updatedStats <- database.accountStats.getByAccount(accountGlobalStatsData.head.account)
            } yield (existingStats, updated, updatedStats)

            val expectedUpdatedStats = AccountGlobalStats("FOO1234", 4.0, 8.0, 2)

            futureStats map { maybeStats =>
                println("Got futures")
                println("Stats existed: " + maybeStats._1.isDefined)
                println("Existing amounts: " + maybeStats._1.get.sumAmounts)
                println("Stats were updated: " + maybeStats._2)
                println("Updated stats exist: " + maybeStats._3.isDefined)
                println("Updated amounts: " + maybeStats._3.get.sumAmounts)
                assert(maybeStats._3.isDefined, "Updated stats shouldn't be empty")
            }
        }
    }
}
