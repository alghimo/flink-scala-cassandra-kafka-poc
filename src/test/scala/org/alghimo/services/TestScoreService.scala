package org.alghimo.services

import org.alghimo.cassandra.TestDatabaseProvider

/**
  * Created by alghimo on 11/20/2016.
  */
object TestScoreService extends ConcreteScoreService with TestDatabaseProvider

trait TestScoreService extends ScoreServiceProvider {
    override def scoreService: ConcreteScoreService = TestScoreService
}
