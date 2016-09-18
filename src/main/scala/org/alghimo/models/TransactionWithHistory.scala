package org.alghimo.models

/**
  * Created by alghimo on 9/12/2016.
  */
case class TransactionWithHistory(
    id: Long,
    srcAccount: String,
    dstAccount: String,
    amount: Double,
    srcCountry: String,
    dstCountry: String,
    srcBank: String,
    dstBank: String,
    accountStats: Option[HistoricalStats],
    globalStats: Option[HistoricalStats],
    countryStats: Option[HistoricalStats],
    created: Long
)
