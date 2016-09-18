package org.alghimo.models

/**
  * Created by alghimo on 9/12/2016.
  */
case class BaseTransaction(id: Long, srcAccount: String, dstAccount: String, amount: Double, created: Long = System.nanoTime())
