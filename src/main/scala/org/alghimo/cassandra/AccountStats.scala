package org.alghimo.cassandra

import com.websudos.phantom.dsl._
import org.alghimo.models.AccountGlobalStats

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class AccountStats extends CassandraTable[ConcreteAccountStats, AccountGlobalStats]{

    object account       extends StringColumn(this) with PartitionKey[String]
    object sumAmounts    extends DoubleColumn(this)
    object sumAmountsSqr extends DoubleColumn(this)
    object numTransacs   extends LongColumn(this)

    def fromRow(row: Row): AccountGlobalStats = {
        AccountGlobalStats(
            account(row),
            sumAmounts(row),
            sumAmountsSqr(row),
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountStats extends AccountStats with RootConnector {
    def store(t: AccountGlobalStats): Future[Boolean] = {

        val statsPromise = Promise[Boolean]()
        val currentStats = getByAccount(t.account)

        def handleUpdate(f: Future[ResultSet], isInsert: Boolean) = {
            f.onComplete({
                case Failure(ex)             => statsPromise.failure(ex)
                case Success(rst: ResultSet) => statsPromise.success(isInsert)
            })
        }
        currentStats.onComplete({
            case Failure(ex)   => statsPromise.failure(ex)
            case Success(None) =>
                val res = update()
                    .where(_.account eqs t.account)
                    .modify(_.sumAmounts setTo t.sumAmounts)
                    .and(_.sumAmountsSqr setTo t.sumAmountsSqr)
                    .and(_.numTransacs setTo t.numTransacs)
                    .consistencyLevel_=(ConsistencyLevel.ALL)
                    .future()

                handleUpdate(res, isInsert = true)
            case Success(Some(accStats: AccountGlobalStats)) =>
                val res = update()
                    .where(_.account eqs t.account)
                    .modify(_.sumAmounts setTo (accStats.sumAmounts + t.sumAmounts))
                    .and(_.sumAmountsSqr setTo (accStats.sumAmountsSqr + t.sumAmountsSqr))
                    .and(_.numTransacs setTo (accStats.numTransacs + t.numTransacs))
                    .consistencyLevel_=(ConsistencyLevel.ALL)
                    .future()

                handleUpdate(res, isInsert = false)
        })

        statsPromise.future
    }

    def getByAccount(src: String): Future[Option[AccountGlobalStats]] = {
        select
            .where(_.account eqs src)
            .consistencyLevel_=(ConsistencyLevel.ALL)
            .one()
    }
}
