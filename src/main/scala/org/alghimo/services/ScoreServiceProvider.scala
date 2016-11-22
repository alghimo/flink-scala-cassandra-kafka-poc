package org.alghimo.services

/**
  * Created by alghimo on 11/20/2016.
  */
trait ScoreServiceProvider {
    def scoreService: ConcreteScoreService
}
