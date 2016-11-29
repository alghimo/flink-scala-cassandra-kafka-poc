package org.alghimo

import java.util.Properties

import com.google.gson.Gson
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer09, FlinkKafkaProducer09}
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
/**
  * Created by alghimo on 9/13/2016.
  */
trait KafkaProperties extends Configurable with java.io.Serializable {
    type FlinkKafkaConsumer = FlinkKafkaConsumer09[String]
    type FlinkKafkaProducer = FlinkKafkaProducer09[String]
    protected def gson: Gson
    protected def TRANSACTIONS_TO_SCORE_TOPIC: String
    protected def SCORED_TRANSACTIONS_TOPIC: String
    protected def kafkaSchema: SimpleStringSchema
    protected def kafkaProperties: Properties

    protected def kafkaStringProducer(topic: String) = new FlinkKafkaProducer(topic, kafkaSchema, kafkaProperties)

    protected def kafkaStringConsumer(topic: String) = new FlinkKafkaConsumer(topic, kafkaSchema, kafkaProperties)
}

trait ProductionKafkaProperties extends KafkaProperties {
    override protected def gson                        = new Gson()
    override protected def TRANSACTIONS_TO_SCORE_TOPIC = config.getString("kafka.topics.incomingTransactions")
    override protected def SCORED_TRANSACTIONS_TOPIC   = config.getString("kafka.topics.scoredTransactions")
    override protected def kafkaSchema                 = new SimpleStringSchema
    override protected def kafkaProperties             = {
        val p = new Properties()
        p.setProperty("bootstrap.servers", config.getString("kafka.broker"))
        p.setProperty("group.id", config.getString("kafka.groupid"))
        p.setProperty("auto.offset.reset", config.getString("kafka.offsetPolicy"))
        p
    }
}
