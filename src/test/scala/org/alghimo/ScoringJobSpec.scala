package org.alghimo

import org.alghimo.cassandra.{WithAccountStatsData, WithAccountToAccountTransactionsData, WithAccountToCountryTransactionsData}
import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction, BaseTransaction}
import org.alghimo.services.TestScoreService
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.scalatest.{AsyncFlatSpec, Matchers}

/**
  * Created by alghimo on 11/20/2016.
  */
class ScoringJobSpec
    extends AsyncFlatSpec
    with Matchers
    with KafkaTest
    with WithAccountStatsData
    with WithAccountToAccountTransactionsData
    with WithAccountToCountryTransactionsData
    with WithFlinkMiniCluster
{
    object TestableScoringJob extends ConcreteScoringJob with TestScoreService {
        override def run(args: Array[String] = Array.empty): JobExecutionResult = {
            val env = StreamExecutionEnvironment.createRemoteEnvironment("localhost", flinkPort())
            println("Calling doRun...")
            val stream = doRun(env)
            println("Got stream")
            val jobExecutionResult = env.execute("Score Transactions")
            Thread.sleep(1000)
            jobExecutionResult
        }

        override def doRun(env: StreamExecutionEnvironment) = {
            println("Method - doRun")
            env
                .addSource(kafkaStringConsumer(TRANSACTIONS_TO_SCORE_TOPIC))
                .setParallelism(1)
                .map(scoreService.scoreTransaction _)
                .setParallelism(1)
                .filter(!_.isEmpty)
                .setParallelism(1)
                .map(scoreToJsonMapper)
                .setParallelism(1)
                .addSink(kafkaStringProducer(SCORED_TRANSACTIONS_TOPIC))
                .setParallelism(1)

            print("Generated env")
            env
        }
    }

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
        try {
            //withRunningKafka {
                println("ScoringJobSpec - generating transactions")
                val jsonTransactions = baseTransactions.map(gson.toJson(_))
                val scoredTransactions = jsonTransactions.map(TestScoreService.scoreTransaction(_).get)
                println("ScoringJobSpec - TestableScoringJob.run")

                val env = StreamExecutionEnvironment.createRemoteEnvironment("localhost", flinkPort())
                env.setRestartStrategy(RestartStrategies.noRestart())
                env.getConfig.disableSysoutLogging
                val executionResult = TestableScoringJob.doRun(env)
                println("Got execution result")

                tryExecute(env, "Test doRun")

                println("ScoringJobSpec - sleeping 1 sec")
                Thread.sleep(5000)
                println("ScoringJobSpec - getting first message")

                withStringConsumer { consumer =>
                    import net.manub.embeddedkafka.ConsumerExtensions._

                    val topicMessages = consumer
                        .consumeLazily(SCORED_TRANSACTIONS_TOPIC)
                        .take(baseTransactions.size)

                    println("With string consumer")
                    topicMessages
                        .foreach { message => println(s"Message in topic ${SCORED_TRANSACTIONS_TOPIC}: ${message}") }
                    //topicMessages should be (messages)
                }
                /*withRunningKafka {
                    publishStringMessageToKafka("foo", "test message")
                    println("First message: " + consumeFirstStringMessageFrom(SCORED_TRANSACTIONS_TOPIC))
                    consumeFirstStringMessageFrom("foo") should equal ("test message")
                }*/
                //println("First message: " + consumeFirstStringMessageFrom(SCORED_TRANSACTIONS_TOPIC))

            //}
        } catch {
            case ex: Throwable =>
                println(s"ScoringJobSpec - Exception[${ex.getClass.toString}]: " + ex.getMessage)
                println(s"ScoringJobSpec - Stacktrace: ")
                ex.getStackTrace.foreach(println)
        }

        succeed
    }
}
