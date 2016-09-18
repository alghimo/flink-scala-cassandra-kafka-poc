package org.alghimo.cassandra

import com.websudos.phantom.connectors.ContactPoints
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

/**
  * Created by alghimo on 9/12/2016.
  */
object Defaults {
    val config    = ConfigFactory.load()
    val hosts     = config.getStringList("cassandra.hosts").toList
    val keySpace  = config.getString("cassandra.keyspace")
    val connector = ContactPoints(hosts).keySpace(keySpace)
}
