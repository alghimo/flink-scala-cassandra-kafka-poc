package org.alghimo.services

import org.alghimo.cassandra.TestDatabaseProvider

/**
  * Created by alghimo on 11/20/2016.
  */
object TestScoreService extends ConcreteScoreService with TestDatabaseProvider with java.io.Serializable

trait TestScoreService extends ScoreServiceProvider with java.io.Serializable {
    override def scoreService: ConcreteScoreService = TestScoreService
}
