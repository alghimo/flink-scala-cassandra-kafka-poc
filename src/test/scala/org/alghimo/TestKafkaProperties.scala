package org.alghimo

import java.util.Properties

import com.google.gson.Gson
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

/**
  * Created by alghimo on 11/18/2016.
  */
trait TestKafkaProperties extends KafkaProperties with java.io.Serializable {
    def KAFKA_PORT     = 6667
    def ZOOKEEPER_PORT = 6000

    override protected def gson                        = new Gson()
    override protected def TRANSACTIONS_TO_SCORE_TOPIC = "test_transactions"
    override protected def SCORED_TRANSACTIONS_TOPIC   = "test_scored_transactions"
    override protected def kafkaSchema                 = new SimpleStringSchema
    override protected def kafkaProperties             = {
        val p = new Properties()

        p.setProperty("group.id", "flink-kafka-test")
        p.setProperty("zookeeper.connect", s"localhost:${ZOOKEEPER_PORT}")
        p.setProperty("bootstrap.servers", s"localhost:${KAFKA_PORT}")
        p.setProperty("auto.offset.reset", "earliest")
        p.setProperty("enable.auto.commit", "true")
        p.setProperty("auto.commit.interval.ms", "1000")
        p.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        p.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        p.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        p.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

        p
    }
}
