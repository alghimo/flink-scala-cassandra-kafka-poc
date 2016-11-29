package org.alghimo

import info.batey.kafka.unit.KafkaUnit
import kafka.common.KafkaException
import kafka.producer.KeyedMessage
import kafka.server.KafkaConfig
import org.apache.kafka.common.protocol.Errors

import scala.collection.JavaConversions._
//import net.manub.embeddedkafka.{Consumers, EmbeddedKafka, EmbeddedKafkaConfig}

/**
  * Created by alghimo on 11/20/2016.
  */
trait KafkaTest
    extends BeforeAndAfterEachFixtures
    with TestKafkaProperties
{
    def kafkaMessages: Map[String, Seq[String]] = Map()

    lazy val kafkaUnitServer = new KafkaUnit(s"localhost:${ZOOKEEPER_PORT}", s"localhost:${KAFKA_PORT}")

    def kafkaConfig = KafkaConfig.fromProps(kafkaProperties)

    val errorsCallback = (errors: scala.collection.Map[String, Errors]) => {
        if (!errors.isEmpty) {
            throw new KafkaException("Unable to create topics. Errors: " + errors.valuesIterator.toArray.mkString("\n"))
        }
    }

    override def setupFixtures(): Unit = {
        super.setupFixtures()
        try {
            kafkaUnitServer.startup()

            for (topic <- kafkaMessages.keySet) {
                kafkaUnitServer.createTopic(topic)
            }


            for ((topic, messages) <- kafkaMessages) {
                val keyedMessages = messages.map { message =>
                    new KeyedMessage[String, String](topic, message)
                }
                kafkaUnitServer.sendMessages(keyedMessages.head, keyedMessages.tail:_*)
            }
        } catch {
            case e: Exception => fail(s"Error preparing kafka fixtures - exception [${e.getClass.toString}]: ${e.getMessage}")
        }
    }

    override def cleanupFixtures(): Unit = {
        try {
            //kafkaServer.adminManager.deleteTopics(5000, kafkaMessages.keySet, errorsCallback)
            kafkaUnitServer.shutdown()
        } catch {
            case e: Exception => fail(s"Error cleaning up kafka fixtures - Got exception [${e.getClass.toString}]: ${e.getMessage}")
        }
        super.cleanupFixtures()
    }

    def getMessagesInTopic(topic: String, expectedMessages: Int): Seq[String] = {
        kafkaUnitServer.readMessages(topic, expectedMessages)
    }
}
