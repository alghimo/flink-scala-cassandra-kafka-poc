package org.alghimo

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.websudos.phantom.dsl
import org.alghimo.cassandra.FraudPocDatabase
import org.alghimo.models.{AccountGlobalStats, AccountToAccountTransaction, AccountToCountryTransaction, TransactionScore}
import org.alghimo.services.AccountsService
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.accumulators.LongCounter
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * Created by alghimo on 9/13/2016.
  */
object UpdateHistoryJob extends KafkaProperties with Configurable {
    private final val failedLogger = LoggerFactory.getLogger("org.alghimo.fraudpoc.history.failedUpdates")

    def run(args: Array[String] = Array.empty): JobExecutionResult = {
        val env = StreamExecutionEnvironment.getExecutionEnvironment

        env
            .addSource(kafkaStringConsumer(SCORED_TRANSACTIONS_TOPIC))
            .map(gson.fromJson(_, classOf[TransactionScore]))
            .filter(_.score < 0.8)
            .map(updateStatsFromScore)

        // execute program
        env.execute("Update Historical Data - Transactions")
    }

    private final val updateStatsFromScore = new RichMapFunction[TransactionScore, Unit]() {
        import dsl.context
        private val savedScores       = new LongCounter()
        private val numInserted       = new LongCounter()
        private val numUpdated        = new LongCounter()
        private val failedSaves       = new LongCounter()
        private lazy val startTime    = System.currentTimeMillis()
        private var lastIntervalCount = 0L
        private var statsScheduled    = false

        private final val task = (accum: LongCounter) => new Runnable {
            private final val savedLogger = LoggerFactory.getLogger("org.alghimo.fraudpoc.history.savedPerSecond")
            def run() = {
                if (accum.getLocalValue > lastIntervalCount) {
                    val ellapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    savedLogger.info("saved last second: " + (accum.getLocalValue - lastIntervalCount) + " - AVG: " + (accum.getLocalValue / ellapsed) + " - inserts: " + numInserted.getLocalValue + " - updates: " + numUpdated.getLocalValue)
                    lastIntervalCount = accum.getLocalValue
                }
            }
        }

        override def open(parameters: Configuration): Unit = getRuntimeContext.addAccumulator("saved-scores", savedScores)

        def map(s: TransactionScore) = {
            if (!statsScheduled) {
                doSchedule(this.savedScores)
            }

            val acc2AccStatsFuture = FraudPocDatabase.accountToAccountTransactions.store(
                AccountToAccountTransaction(s.srcAccount, s.dstAccount, s.amount, s.amount * s.amount, 1)
            )
            handleFuture(acc2AccStatsFuture, "Account to Account")

            val accStatsFuture = FraudPocDatabase.accountStats.store(AccountGlobalStats(s.srcAccount, s.amount, s.amount * s.amount, 1))
            handleFuture(accStatsFuture, "Account")

            val country = AccountsService.country(s.dstAccount)
            val acc2CountryStatsFuture = FraudPocDatabase.accountToCountryTransactions.store(AccountToCountryTransaction(s.srcAccount, country, s.amount, s.amount * s.amount, 1))
            handleFuture(acc2CountryStatsFuture, "Account to Country")
        }

        def handleFuture(f: Future[Boolean], element: String) = {
            f.onFailure({
                case ex: Throwable => {
                    failedLogger.info(s"Failed saving ${element} stats")
                    this.failedSaves.add(1)
                }
            })
            f.onSuccess({
                case false => {
                    this.numUpdated.add(1)
                    this.savedScores.add(1)
                }
                case true => {
                    this.numInserted.add(1)
                    this.savedScores.add(1)
                }
            })
        }

        private def doSchedule(accum: LongCounter) = {
            new ScheduledThreadPoolExecutor(1)
                .scheduleAtFixedRate(task(accum), 1, 1, TimeUnit.SECONDS)

            statsScheduled = true
        }
    }
}
