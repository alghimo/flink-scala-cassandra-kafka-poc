package org.alghimo.services

/**
  * Created by alghimo on 11/20/2016.
  */
trait ProductionScoreService extends ScoreServiceProvider {
    override def scoreService = ScoreService
}
