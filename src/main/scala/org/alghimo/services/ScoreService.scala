package org.alghimo.services

import com.google.gson.Gson
import com.websudos.phantom.dsl._
import org.alghimo.cassandra.ProductionDatabase
import org.alghimo.services.AccountsService.{bank, country}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise, TimeoutException}
import scala.util.{Failure, Success}

/**
  * Created by alghimo on 9/1/2016.
  */
class ConcreteScoreService extends ProductionDatabase {
    private final val logger = LoggerFactory.getLogger("org.alghimo.fraudpoc.timeToScore")
    import org.alghimo.models._

    // Compose all the partial functions to score a transaction
    private final lazy val transactionScore = enrichTransaction andThen addHistoricalData andThen getScore
    private final lazy val DEFAULT_STATS = HistoricalStats(0L, 0.0, 0.0)

    // Used to parse json into case classes
    private final val gson = new Gson

    def scoreTransaction(strTransaction: String) = {
        val transaction = gson.fromJson(strTransaction, classOf[BaseTransaction]).copy(created = System.nanoTime())

        transactionScore(transaction)
    }

    private final val enrichTransaction: PartialFunction[BaseTransaction, EnrichedTransaction] = {
        case t: BaseTransaction => EnrichedTransaction(
            id         = t.id,
            srcAccount = t.srcAccount,
            dstAccount = t.dstAccount,
            amount     = t.amount,
            srcCountry = country(t.srcAccount),
            dstCountry = country(t.dstAccount),
            srcBank    = bank(t.srcAccount),
            dstBank    = bank(t.dstAccount),
            created    = t.created
        )
    }

    private final val addHistoricalData: PartialFunction[EnrichedTransaction, Future[TransactionWithHistory]] = {
        case t: EnrichedTransaction => {
            val acc2accStatsFuture     = database.accountToAccountTransactions.getByAccounts(t.srcAccount, t.dstAccount)
            val accStatsFuture         = database.accountStats.getByAccount(t.srcAccount)
            val acc2countryStatsFuture = database.accountToCountryTransactions.getByAccountAndCountry(t.srcAccount, t.dstCountry)
            val allStatsFuture         = for {
                acc2accStats     <- acc2accStatsFuture
                accStats         <- accStatsFuture
                acc2CountryStats <- acc2countryStatsFuture
            } yield (acc2accStats, accStats, acc2CountryStats)

            val transactionWithHistoryPromise: Promise[TransactionWithHistory] = Promise()

            allStatsFuture.onComplete({
                case Failure(ex) => transactionWithHistoryPromise.failure(ex)
                case Success((acc2accOpt, accOpt, acc2countryOpt)) => {
                    val acc2accStats     = acc2accOpt.map(t => HistoricalStats(t.numTransacs, t.sumAmounts, t.sumAmountsSqr))
                    val accStats         = accOpt.map(t => HistoricalStats(t.numTransacs, t.sumAmounts, t.sumAmountsSqr))
                    val acc2countryStats = acc2countryOpt.map(t => HistoricalStats(t.numTransacs, t.sumAmounts, t.sumAmountsSqr))

                    transactionWithHistoryPromise.success(TransactionWithHistory(
                        id           = t.id,
                        srcAccount   = t.srcAccount,
                        dstAccount   = t.dstAccount,
                        amount       = t.amount,
                        srcCountry   = t.srcCountry,
                        dstCountry   = t.dstCountry,
                        srcBank      = t.srcBank,
                        dstBank      = t.dstBank,
                        accountStats = acc2accStats,
                        globalStats  = accStats,
                        countryStats = acc2countryStats,
                        created      = t.created
                    ))
                }
            })

            transactionWithHistoryPromise.future
        }
    }

    private final val getScore: PartialFunction[Future[TransactionWithHistory], Option[TransactionScore]] = {
        case f: Future[TransactionWithHistory] => {
            val scorePromise: Promise[TransactionScore] = Promise()
            f.onComplete({
                case Failure(ex) => scorePromise.failure(ex)
                case Success(t: TransactionWithHistory) => {
                    /**
                      * Calculate risk based on the destination account.
                      * If there are no stats from the source to the destination account,
                      * the global stats for the source account are used to calculate the risk on the amount.
                      */
                    val amountToAccountRisk      = amountRisk(t.amount, t.accountStats.orElse(t.globalStats))
                    val numTransacsToAccountRisk = numTransacsRisk(t.accountStats)
                    val accountRisk              = amountToAccountRisk * 0.8 + numTransacsToAccountRisk * 0.2

                    /**
                      * Calculate risk based on the destination country.
                      * If there are no stats from the source to the destination country,
                      * the global stats for the source account are used to calculate the risk on the amount.
                      */
                    val amountToCountryRisk      = amountRisk(t.amount, t.countryStats.orElse(t.globalStats))
                    val numTransacsToCountryRisk = numTransacsRisk(t.countryStats)
                    val countryRisk              = amountToCountryRisk * 0.8 + numTransacsToCountryRisk * 0.2

                    val now = System.nanoTime()
                    logger.info(s"ID: ${t.id} - Time to score: ${"%.2f".format((now - t.created)/1000000.0)}")

                    scorePromise.success(TransactionScore(
                        id                = t.id,
                        srcAccount        = t.srcAccount,
                        dstAccount        = t.dstAccount,
                        amount            = t.amount,
                        score             = accountRisk * 0.5 + countryRisk * 0.5,
                        timeToScoreMillis = "%.2f".format((System.nanoTime() - t.created) / 1000000.0)
                    ))
                }
            })

            /**
              * Flink doesn't have a way to handle maps that happen asynchronously, i.e.: returning a future / promise.
              */
            try {
                Some(Await.result(scorePromise.future, 1 second))
            } catch {
                case e: TimeoutException => {
                    logger.info("Timedout scoring transaction")
                    None
                }
                case e: InterruptedException => {
                    logger.info("Interrupted while scoring transaction")
                    None
                }
            }
        }
    }

    private def amountRisk(amount:Double, mayBeStats: Option[HistoricalStats]): Double = {
        mayBeStats match {
            case None => 1.0
            case Some(stats) => {
                val avgAmountToAccount = stats.sumAmounts / stats.numTransacs
                // sqrt( (sum(x^2)/N) - (mean(x))^2 )
                val stddevToAccount = Math.sqrt(stats.sumAmountsSqr / stats.numTransacs - Math.pow(stats.sumAmounts / stats.numTransacs, 2))

                val numStdDevs: Double = (amount - avgAmountToAccount) / stddevToAccount

                /**
                  * Chebyshev's inequality.
                  * Given m=mean, s=stddev and k=number of std devs, AT LEAST a percentage of (1 - 1/k ^ 2)
                  * will be contained within the interval m +- ks.
                  * If the amount has at least 90% of amounts below, we consider it risky (1.0).
                  * If it has at least 80% of amounts below, we will interpolate it linearly (0.8=0.0, 0.85=0.5, 0.90=1.0)
                  */
                val minPercentOfTransactionsBelow: Double = 1 - 1 / (numStdDevs * numStdDevs)
                if (minPercentOfTransactionsBelow > 0.9) {
                    1.0
                } else if (minPercentOfTransactionsBelow > 0.8) {
                    // Ex: 0.81 transactions above => 10% risk (0.1)
                    // Ex: 0.89 transactions above => 90% risk (0.9)
                    (minPercentOfTransactionsBelow - 0.8) * 10.0
                } else {
                    0.0
                }
            }
        }
    }

    private def numTransacsRisk(maybeStats: Option[HistoricalStats]) = {
        maybeStats match {
            case None => 1.0
            case Some(stats) if stats.numTransacs < 10 => 1.0 - stats.numTransacs * 0.1
            case _ => 0.0
        }
    }
}

object ScoreService extends ConcreteScoreService