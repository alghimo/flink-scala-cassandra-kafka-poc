package org.alghimo.cassandra

import com.typesafe.config.ConfigFactory
import com.websudos.phantom.connectors.ContactPoint

/**
  * Created by alghimo on 11/13/2016.
  */
object TestDefaults {
    val config    = ConfigFactory.load()
    val keySpace  = config.getString("cassandra.keyspace")
    val connector = ContactPoint.embedded.noHeartbeat().keySpace(keySpace)
}
