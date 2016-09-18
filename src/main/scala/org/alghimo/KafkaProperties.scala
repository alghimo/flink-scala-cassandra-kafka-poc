package org.alghimo

import java.util.Properties

import com.google.gson.Gson
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer09, FlinkKafkaProducer09}
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

/**
  * Created by alghimo on 9/13/2016.
  */
trait KafkaProperties extends Configurable {
    protected final val gson                        = new Gson()
    protected final val TRANSACTIONS_TO_SCORE_TOPIC = config.getString("kafka.topics.incomingTransactions")
    protected final val SCORED_TRANSACTIONS_TOPIC   = config.getString("kafka.topics.scoredTransactions")
    private final val kafkaSchema                   = new SimpleStringSchema
    protected val kafkaProperties                   = {
        val p = new Properties()
        p.setProperty("bootstrap.servers", config.getString("kafka.broker"))
        p.setProperty("group.id", config.getString("kafka.groupid"))
        p.setProperty("auto.offset.reset", config.getString("kafka.offsetPolicy"))
        p
    }

    protected def kafkaStringProducer(topic: String) = new FlinkKafkaProducer09[String](topic, kafkaSchema, kafkaProperties)

    protected def kafkaStringConsumer(topic: String) = new FlinkKafkaConsumer09[String](topic, kafkaSchema, kafkaProperties)
}
