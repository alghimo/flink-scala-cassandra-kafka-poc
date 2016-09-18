package org.alghimo.cassandra

import com.websudos.phantom.dsl._
import org.alghimo.models.AccountToCountryTransaction

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by alghimo on 9/12/2016.
  */
class AccountToCountryTransactions  extends CassandraTable[ConcreteAccountToCountryTransactions, AccountToCountryTransaction]{
    object srcAccount    extends StringColumn(this) with PrimaryKey[String]
    object dstCountry    extends StringColumn(this) with PrimaryKey[String]
    object sumAmounts    extends DoubleColumn(this)
    object sumAmountsSqr extends DoubleColumn(this)
    object numTransacs   extends LongColumn(this)

    def fromRow(row: Row): AccountToCountryTransaction = {
        AccountToCountryTransaction(
            srcAccount(row),
            dstCountry(row),
            sumAmounts(row),
            sumAmountsSqr(row),
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountToCountryTransactions extends AccountToCountryTransactions with RootConnector {
    def store(t: AccountToCountryTransaction): Future[Boolean] = {
        val statsPromise = Promise[Boolean]()
        val currentStats = getByAccountAndCountry(t.srcAccount, t.dstCountry)

        def handleUpdate(f: Future[ResultSet], isInsert: Boolean) = {
            f.onComplete({
                case Failure(ex)             => statsPromise.failure(ex)
                case Success(rst: ResultSet) => statsPromise.success(isInsert)
            })
        }
        currentStats.onComplete({
            case Failure(ex)   => statsPromise.failure(ex)
            case Success(None) => {
                val res = insert()
                    .value(_.srcAccount, t.srcAccount)
                    .value(_.dstCountry, t.dstCountry)
                    .value(_.sumAmounts, t.sumAmounts)
                    .value(_.sumAmountsSqr, t.sumAmountsSqr)
                    .value(_.numTransacs, t.numTransacs)
                    .future()

                handleUpdate(res, isInsert = true)
            }
            case Success(Some(accStats: AccountToCountryTransaction)) => {
                val res = update()
                    .where(_.srcAccount eqs t.srcAccount)
                    .and(_.dstCountry eqs t.dstCountry)
                    .modify(_.sumAmounts setTo (accStats.sumAmounts + t.sumAmounts))
                    .and(_.sumAmountsSqr setTo (accStats.sumAmountsSqr + t.sumAmountsSqr))
                    .and(_.numTransacs setTo (accStats.numTransacs + t.numTransacs))
                    .future()

                handleUpdate(res, isInsert = false)
            }
        })

        statsPromise.future
    }

    def getByAccountAndCountry(src: String, dst: String): Future[Option[AccountToCountryTransaction]] = {
        select
            .where(_.srcAccount eqs src)
            .and(_.dstCountry eqs dst)
            .one()
    }
}



/*
Version with counters

class AccountToCountryTransactions  extends CassandraTable[ConcreteAccountToCountryTransactions, AccountToCountryTransaction]{
    object srcAccount extends StringColumn(this) with PrimaryKey[String]
    object dstCountry extends StringColumn(this) with PrimaryKey[String]
    object sumAmounts extends CounterColumn(this)
    object sumAmountsSqr extends CounterColumn(this)
    object numTransacs extends CounterColumn(this)

    def fromRow(row: Row): AccountToCountryTransaction = {
        AccountToCountryTransaction(
            srcAccount(row),
            dstCountry(row),
            sumAmounts(row) / 100.0,
            sumAmountsSqr(row) / 100.0,
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountToCountryTransactions extends AccountToCountryTransactions with RootConnector {
    def store(t: AccountToCountryTransaction): Future[ResultSet] = {
        update
            .where(_.srcAccount eqs t.srcAccount)
            .and(_.dstCountry eqs t.dstCountry)
            .modify(_.sumAmounts increment Math.round(t.sumAmounts * 100))
            .and(_.sumAmountsSqr increment Math.round(t.sumAmountsSqr) * 100)
            .and(_.numTransacs increment t.numTransacs)
            .future()
    }

    def getByAccountAndCountry(src: String, dst: String): Future[Option[AccountToCountryTransaction]] = {
        select
            .where(_.srcAccount eqs src)
            .and(_.dstCountry eqs dst)
            .one()
    }
}
 */