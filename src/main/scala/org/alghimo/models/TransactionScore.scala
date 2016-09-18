package org.alghimo.models

/**
  * Created by alghimo on 9/12/2016.
  */
case class TransactionScore(id: Long, srcAccount: String, dstAccount: String, amount: Double, score: Double, timeToScoreMillis: String)
