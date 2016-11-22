package org.alghimo

import net.manub.embeddedkafka.{Consumers, EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncTestSuite, Matchers, OptionValues}

import scala.concurrent.ExecutionContext

/**
  * Created by alghimo on 11/20/2016.
  */
trait KafkaTest
    extends AsyncTestSuite
    with TestKafkaProperties
    with EmbeddedKafka
    with ScalaFutures
    with Matchers
    with OptionValues
    with BeforeAndAfterEachFixtures
    with Consumers
{
    override implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    /*val serialExecutionContext2: ExecutionContext = new org.scalatest.concurrent.SerialExecutionContext
    implicit def executionContext2: ExecutionContext = serialExecutionContext2*/

    def kafkaMessages: Map[String, Seq[String]] = Map()

    override def setupFixtures(): Unit = {
        implicit val config = EmbeddedKafkaConfig(kafkaPort = KAFKA_PORT)
        super.setupFixtures()
        EmbeddedKafka.start()
        for (topic <- kafkaMessages.keySet) {
            println(s"Creating topic - ${topic}")
            createCustomTopic(topic)
        }
        /*createCustomTopic("test_topic")
        publishStringMessageToKafka("test_topic", "test message")
        println("setup - consumed message: " + consumeFirstStringMessageFrom("foo"))*/
        for ((topic, messages) <- kafkaMessages) {
            messages.foreach(publishStringMessageToKafka(topic, _))
        }
    }

    override def cleanupFixtures(): Unit = {
        /*withStringConsumer { consumer =>
            import net.manub.embeddedkafka.ConsumerExtensions._
            val topic = kafkaMessages.keySet.head
            consumer.consumeLazily(topic).foreach(m => println(s"Consumed message from ${topic}: ${m}"))
            val topicMessages = consumer

        }*/
        EmbeddedKafka.stop()
        super.cleanupFixtures()
    }

    def assertMessagesInTopic(topic: String, messages: Seq[String]) = {
        withStringConsumer { consumer =>
            import net.manub.embeddedkafka.ConsumerExtensions._

            val topicMessages = consumer
                .consumeLazily(topic)
                .take(messages.size)

            topicMessages
                .foreach { message => println(s"Message in topic ${topic}: ${message}") }
            topicMessages should be (messages)
        }
    }
}
