package org.alghimo

import org.alghimo.utils.KafkaTest
import org.scalatest.FlatSpec
//import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig, KafkaUnavailableException}
import org.alghimo.cassandra.{WithAccountStatsData, WithAccountToAccountTransactionsData, WithAccountToCountryTransactionsData}
import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction, BaseTransaction}
import org.alghimo.services.TestScoreService
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.scalatest.Matchers

/**
  * Created by alghimo on 11/20/2016.
  */
class ScoringJobSpec
    extends FlatSpec
    with Matchers
    with KafkaTest
    with WithAccountStatsData
    with WithAccountToAccountTransactionsData
    with WithAccountToCountryTransactionsData
    with java.io.Serializable
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

    val baseTransactions = Seq(
        BaseTransaction(id = 1L, srcAccount = "BENW300654", dstAccount = "BEXX5432", amount = 10.0),
        BaseTransaction(id = 2L, srcAccount = "BEXX300", dstAccount = "BEXX4321", amount = 20.0),
        BaseTransaction(id = 3L, srcAccount = "NLXX300", dstAccount = "NLXX6543", amount = 7.0),
        BaseTransaction(id = 4L, srcAccount = "NLXX300", dstAccount = "ESXX1234", amount = 5.0)
    )

    override def kafkaMessages: Map[String, Seq[String]] = Map(
        TRANSACTIONS_TO_SCORE_TOPIC -> baseTransactions.map(gson.toJson(_))
    )

    behavior of "ConcreteScoringJob"

    it should "consume the transactions from kafka and publish the scored transactions" in {
        val testableJob = new AbstractScoringJob with TestKafkaProperties with TestScoreService with java.io.Serializable {
            override protected def getExecutionEnv(): StreamExecutionEnvironment = StreamExecutionEnvironment.createLocalEnvironment(1)
        }
        val jsonTransactions   = baseTransactions.map(gson.toJson(_))
        val scoredTransactions = jsonTransactions.map(TestScoreService.scoreTransaction(_).get)

        testableJob.run()
        //TestableScoringJob.run()

        val generatedMessages = getMessagesInTopic(SCORED_TRANSACTIONS_TOPIC, scoredTransactions.size)
        generatedMessages should contain allElementsOf scoredTransactions
    }
}
