package org.alghimo

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.client.program.ProgramInvocationException
import org.apache.flink.configuration.{ConfigConstants, Configuration}
import org.apache.flink.runtime.client.JobExecutionException
import org.apache.flink.runtime.minicluster.LocalFlinkMiniCluster
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

class SuccessException extends Exception {
    private final val serialVersionUID = -7011865671593955887L
}

/**
  * Created by alghimo on 11/21/2016.
  */
trait WithFlinkMiniCluster extends BeforeAndAfterEachFixtures {
    lazy val flink = new LocalFlinkMiniCluster(getFlinkConfiguration(), false)
    def flinkPort() = flink.getLeaderRPCPort

    protected def getFlinkConfiguration() = {
        val flinkConfig = new Configuration();
        flinkConfig.setInteger(ConfigConstants.LOCAL_NUMBER_TASK_MANAGER, 1)
        flinkConfig.setInteger(ConfigConstants.TASK_MANAGER_NUM_TASK_SLOTS, 8)
        flinkConfig.setInteger(ConfigConstants.TASK_MANAGER_MEMORY_SIZE_KEY, 16)
        flinkConfig.setString(ConfigConstants.RESTART_STRATEGY_FIXED_DELAY_DELAY, "0 s")
        //flinkConfig.setString(ConfigConstants.METRICS_REPORTERS_LIST, "my_reporter");
        //flinkConfig.setString(ConfigConstants.METRICS_REPORTER_PREFIX + "my_reporter." + ConfigConstants.METRICS_REPORTER_CLASS_SUFFIX, import org.apache.flink.metrics.jmx.JMXReporter.class.getName());
        flinkConfig
    }

    override def setupFixtures(): Unit = {
        super.setupFixtures()
        println("Starting flink minicluster")
        flink.start()
    }

    override def cleanupFixtures(): Unit = {
        println("Shutting down flink minicluster")
        flink.stop()
        super.cleanupFixtures()
    }

    def tryExecute(see: StreamExecutionEnvironment, name: String): JobExecutionResult = {
        var isSuccessException = false
        try {
            println("Trying to execute... " + name)
            return see.execute(name)
        } catch {
            case root: Exception if root.isInstanceOf[ProgramInvocationException] || root.isInstanceOf[JobExecutionException] =>
                println(s"Got exception of type ${root.getClass.toString}")
                var cause = root.getCause
                isSuccessException = (cause != null) && cause.isInstanceOf[SuccessException]
                // search for nested SuccessExceptions
                var depth = 0
                while (!isSuccessException) {
                    depth = depth + 1
                    if (cause == null || depth == 20) {
                        root.printStackTrace();
                        fail("Test failed: " + root.getMessage)
                    } else {
                        cause = cause.getCause
                    }

                    isSuccessException = (cause != null) && cause.isInstanceOf[SuccessException]
                }
        }

        null
    }
}
