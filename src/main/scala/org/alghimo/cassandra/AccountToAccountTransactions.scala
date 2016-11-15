package org.alghimo.cassandra

import com.websudos.phantom.dsl._
import org.alghimo.models.AccountToAccountTransaction

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by alghimo on 9/12/2016.
  */
class AccountToAccountTransactions extends CassandraTable[ConcreteAccountToAccountTransactions, AccountToAccountTransaction]{
    object srcAccount    extends StringColumn(this) with PartitionKey[String]
    object dstAccount    extends StringColumn(this) with PrimaryKey[String]
    object sumAmounts    extends DoubleColumn(this)
    object sumAmountsSqr extends DoubleColumn(this)
    object numTransacs   extends LongColumn(this)

    def fromRow(row: Row): AccountToAccountTransaction = {
        AccountToAccountTransaction(
            srcAccount(row),
            dstAccount(row),
            sumAmounts(row),
            sumAmountsSqr(row),
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountToAccountTransactions extends AccountToAccountTransactions with RootConnector {
    def store(t: AccountToAccountTransaction): Future[Boolean] = {
        val statsPromise = Promise[Boolean]()
        val currentStats = getByAccounts(t.srcAccount, t.dstAccount)

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
                    .value(_.dstAccount, t.dstAccount)
                    .value(_.sumAmounts, t.sumAmounts)
                    .value(_.sumAmountsSqr, t.sumAmountsSqr)
                    .value(_.numTransacs, t.numTransacs)
                    .future()

                handleUpdate(res, isInsert = true)
            }
            case Success(Some(accStats: AccountToAccountTransaction)) => {
                val res = update()
                    .where(_.srcAccount eqs t.srcAccount)
                    .and(_.dstAccount eqs t.dstAccount)
                    .modify(_.sumAmounts setTo (accStats.sumAmounts + t.sumAmounts))
                    .and(_.sumAmountsSqr setTo (accStats.sumAmountsSqr + t.sumAmountsSqr))
                    .and(_.numTransacs setTo (accStats.numTransacs + t.numTransacs))
                    .future()

                handleUpdate(res, isInsert = false)
            }
        })

        statsPromise.future
    }

    def getByAccounts(src: String, dst: String): Future[Option[AccountToAccountTransaction]] = {
        select
            .where(_.srcAccount eqs src)
            .and(_.dstAccount eqs dst)
            .one()
    }
}

/*
Version with counters
class AccountToAccountTransactions extends CassandraTable[ConcreteAccountToAccountTransactions, AccountToAccountTransaction]{
    object srcAccount extends StringColumn(this) with PrimaryKey[String]
    object dstAccount extends StringColumn(this) with PrimaryKey[String]
    object sumAmounts extends CounterColumn(this)
    object sumAmountsSqr extends CounterColumn(this)
    object numTransacs extends CounterColumn(this)

    def fromRow(row: Row): AccountToAccountTransaction = {
        AccountToAccountTransaction(
            srcAccount(row),
            dstAccount(row),
            sumAmounts(row) / 100.0,
            sumAmountsSqr(row) / 100.0,
            numTransacs(row)
        )
    }
}

abstract class ConcreteAccountToAccountTransactions extends AccountToAccountTransactions with RootConnector {
    def store(t: AccountToAccountTransaction): Future[ResultSet] = {
        update
            .where(_.srcAccount eqs t.srcAccount)
            .and(_.dstAccount eqs t.dstAccount)
            .modify(_.sumAmounts increment Math.round(t.sumAmounts * 100))
            .and(_.sumAmountsSqr increment Math.round(t.sumAmountsSqr) * 100)
            .and(_.numTransacs increment t.numTransacs)
            .future()
    }

    def getByAccounts(src: String, dst: String): Future[Option[AccountToAccountTransaction]] = {
        select
            .where(_.srcAccount eqs src)
            .and(_.dstAccount eqs dst)
            .one()
    }
}
 */