package org.alghimo.cassandra

import com.websudos.phantom.connectors.ContactPoint
import org.alghimo.Configurable

/**
  * Created by alghimo on 11/13/2016.
  */
object TestDefaults extends Configurable {
    lazy protected val keySpace  = config.getString("cassandra.keyspace")
    lazy protected val connector = ContactPoint.embedded.noHeartbeat().keySpace(keySpace)
}
