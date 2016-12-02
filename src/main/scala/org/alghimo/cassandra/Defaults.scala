package org.alghimo.cassandra

import com.websudos.phantom.connectors.ContactPoints
import org.alghimo.Configurable

import scala.collection.JavaConversions._

/**
  * Created by alghimo on 9/12/2016.
  */
object Defaults extends Configurable {
    protected lazy val hosts     = config.getStringList("cassandra.hosts").toList
    protected lazy val keySpace  = config.getString("cassandra.keyspace")
    protected lazy val connector = ContactPoints(hosts).keySpace(keySpace)
}
