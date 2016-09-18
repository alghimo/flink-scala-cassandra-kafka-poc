package org.alghimo

import com.typesafe.config.ConfigFactory

/**
  * Created by alghimo on 9/13/2016.
  */
trait Configurable {
    lazy val config = ConfigFactory.load()
}
