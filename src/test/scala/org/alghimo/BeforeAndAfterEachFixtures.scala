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
                println("Error running test with fixture: " + ex.getMessage)

                throw ex
            }// Invoke the test function
        } lastly {
            cleanupFixtures()
        }
    }

    def setupFixtures(): Unit = {
    }

    def cleanupFixtures(): Unit = {
    }
}
