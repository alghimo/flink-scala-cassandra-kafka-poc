package org.alghimo

import org.scalatest.AsyncTestSuite

/**
  * Created by alghimo on 11/20/2016.
  */
trait BeforeAndAfterEachFixtures extends AsyncTestSuite {
    override def withFixture(test: NoArgAsyncTest) = {

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
