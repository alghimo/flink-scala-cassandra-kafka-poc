package org.alghimo.cassandra

import com.websudos.phantom.dsl._
import org.alghimo.models.AccountGlobalStats

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class AccountStats extends CassandraTable[ConcreteAccountStats, AccountGlobalStats]{

    object account       extends StringColumn(this) with PrimaryKey[String]
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
            case Success(None) => {
                println("No previous stats - inserting for account " + t.account)
                val res = insert()
                    .value(_.account, t.account)
                    .value(_.sumAmounts, t.sumAmounts)
                    .value(_.sumAmountsSqr, t.sumAmountsSqr)
                    .value(_.numTransacs, t.numTransacs)
                    .future()

                handleUpdate(res, isInsert = true)
            }
            case Success(Some(accStats: AccountGlobalStats)) => {
                val res = update()
                    .where(_.account eqs t.account)
                    .modify(_.sumAmounts setTo (accStats.sumAmounts + t.sumAmounts))
                    .and(_.sumAmountsSqr setTo (accStats.sumAmountsSqr + t.sumAmountsSqr))
                    .and(_.numTransacs setTo (accStats.numTransacs + t.numTransacs))
                    .future()

                handleUpdate(res, isInsert = false)
            }
        })

        statsPromise.future
    }

    def getByAccount(src: String): Future[Option[AccountGlobalStats]] = {
        select
            .where(_.account eqs src)
            .one()
    }
}

/*
// VERSION WITH COUNTERS
class AccountStats extends CassandraTable[ConcreteAccountStats, AccountGlobalStats]{

    object account extends StringColumn(this) with PrimaryKey[String]
    object sumAmounts extends CounterColumn(this)
    object sumAmountsSqr extends CounterColumn(this)
    object numTransacs extends CounterColumn(this)

    def fromRow(row: Row): AccountGlobalStats = {
        AccountGlobalStats(
            account(row),
            sumAmounts(row) / 100.0,
            sumAmountsSqr(row) / 100.0,
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountStats extends AccountStats with RootConnector {
    def store(t: AccountGlobalStats): Future[ResultSet] = {
        update
            .where(_.account eqs t.account)
            .modify(_.sumAmounts increment Math.round(t.sumAmounts * 100))
            .and(_.sumAmountsSqr increment Math.round(t.sumAmountsSqr) * 100)
            .and(_.numTransacs increment t.numTransacs)
            .future()
    }

    def getByAccount(src: String): Future[Option[AccountGlobalStats]] = {
        select
            .where(_.account eqs src)
            .one()
    }
}
*/
