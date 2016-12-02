package org.alghimo.utils

import org.scalatest.{AsyncTestSuite, FutureOutcome}

/**
  * Created by alghimo on 11/20/2016.
  */
trait BeforeAndAfterEachFixtures extends AsyncTestSuite {
    override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

        setupFixtures()

        complete {
            super.withFixture(test) onFailedThen { ex =>
                fail(s"Error running test with fixture. Exception [${ex.getClass.toString}]: ${ex.getMessage}")
            }
        } lastly {
            cleanupFixtures()
        }
    }

    def setupFixtures(): Unit = {
    }

    def cleanupFixtures(): Unit = {
    }
}
