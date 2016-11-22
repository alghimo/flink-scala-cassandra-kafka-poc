package org.alghimo

import java.util.Properties

/**
  * Created by alghimo on 11/18/2016.
  */
trait TestKafkaProperties extends KafkaProperties {
    def KAFKA_PORT = 6667

    override protected val kafkaProperties                   = {
        val p = new Properties()
        p.setProperty("bootstrap.servers", s"localhost:${KAFKA_PORT}")
        p.setProperty("group.id", "embedded-kafka-spec")
        p.setProperty("auto.offset.reset", "earliest")
        p
    }
}
