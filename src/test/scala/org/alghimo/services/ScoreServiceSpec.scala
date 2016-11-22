package org.alghimo.services

import com.google.gson.Gson
import org.alghimo.cassandra._
import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction, BaseTransaction}
import org.scalatest.AsyncFlatSpec

/**
  * Created by alghimo on 11/18/2016.
  */
class ScoreServiceSpec extends AsyncFlatSpec
    with TestScoreService
    with WithAccountStatsData
    with WithAccountToAccountTransactionsData
    with WithAccountToCountryTransactionsData
{
    override def accountGlobalStatsData: Seq[AccountGlobalStats] = Seq(
        AccountGlobalStats(account = "BEXX300", sumAmounts = 10.0, sumAmountsSqr = 100.0, numTransacs = 1),
        AccountGlobalStats(account = "NLXX300", sumAmounts = 65.0, sumAmountsSqr = 421.0, numTransacs = 13)
    )

    override def accountToAccountTransactionsData: Seq[AccountToAccountTransaction] = Seq(
        AccountToAccountTransaction(srcAccount = "BEXX300", dstAccount = "BEXX4321", sumAmounts = 10.0, sumAmountsSqr = 100.0, numTransacs = 1),
        // 2 transactions of values 2 and 4
        AccountToAccountTransaction(srcAccount = "NLXX300", dstAccount = "NLXX9876", sumAmounts = 6.0, sumAmountsSqr = 20.0, numTransacs = 2),
        // 10 transactions of amounts 1 to 10
        AccountToAccountTransaction(srcAccount = "NLXX300", dstAccount = "NLXX6543", sumAmounts = 55.0, sumAmountsSqr = 385.0, numTransacs = 10),
        AccountToAccountTransaction(srcAccount = "NLXX300", dstAccount = "DEXX5764", sumAmounts = 4.0, sumAmountsSqr = 16.0, numTransacs = 1)
    )

    override def accountToCountryTransactionsData: Seq[AccountToCountryTransaction] = Seq(
        AccountToCountryTransaction(srcAccount = "BEXX300", dstCountry = "BE", sumAmounts = 10.0, sumAmountsSqr = 100.0, numTransacs = 1),
        AccountToCountryTransaction(srcAccount = "NLXX300", dstCountry = "NL", sumAmounts = 61.0, sumAmountsSqr = 405.0, numTransacs = 12),
        AccountToCountryTransaction(srcAccount = "NLXX300", dstCountry = "DE", sumAmounts = 4.0, sumAmountsSqr = 16.0, numTransacs = 1)

    )

    behavior of "ScoreService"

    it should "asses the transaction risk" in {
        val gson = new Gson()
        val newTransaction = BaseTransaction(id = 1L, srcAccount = "NLXX300", dstAccount = "NLXX654987", amount = 3.0)
        val maybeScore = scoreService.scoreTransaction(gson.toJson(newTransaction))
        assert(maybeScore.isDefined)
        val scoredTransaction = maybeScore.get

        assert(maybeScore.get.score >= 0.0 && maybeScore.get.score <= 1.0, "Risk for the transaction should be >= 0")
    }

    it should "calculate the risk using account to account and account to country stats when available" in {
        val gson = new Gson()
        val newTransaction = BaseTransaction(id = 1L, srcAccount = "NLXX300", dstAccount = "NLXX6543", amount = 12.0)
        val maybeScore = scoreService.scoreTransaction(gson.toJson(newTransaction))
        assert(maybeScore.isDefined)
        val score = maybeScore.get.score
        // actual score = 0.15759073937440737
        assert(score > 0.1575 && score < 0.1576)
    }

    it should "calculate the risk using account stats as fallback for transactions to new a new account / country" in {
        val gson = new Gson()
        val newTransaction = BaseTransaction(id = 1L, srcAccount = "NLXX300", dstAccount = "NLZZ9876", amount = 12.0)
        val maybeScore = scoreService.scoreTransaction(gson.toJson(newTransaction))
        assert(maybeScore.isDefined)
        val score = maybeScore.get.score
        // actual score = 0.4358300824489153
        assert(score > 0.43582 && score < 0.43584)
    }

    it should "assign 100% risk when there is no historical data" in {
        val gson = new Gson()
        val newTransaction = BaseTransaction(id = 1L, srcAccount = "NLNW300123456", dstAccount = "NLZZ98765432", amount = 10.0)
        val maybeScore = scoreService.scoreTransaction(gson.toJson(newTransaction))
        assert(maybeScore.isDefined)
        assert(maybeScore.get.score == 1.0)
    }
}
