package org.alghimo.services

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alghimo on 11/15/2016.
  */
class AccountsServiceSpec extends FlatSpec with Matchers {
    behavior of "AccountsService"

    it should "extract the country from the first two digits of an account" in {
        val country = AccountsService.country("BE123456")
        country should equal ("BE")
    }

    val accountsToTest = Map(
        "XXXX300999999999" -> "ING",
        "XXXX400999999999" -> "BNP",
        "XXXX500999999999" -> "BEL",
        "XXXX600999999999" -> "KBC",
        "XXXX700999999999" -> "ARG",
        "XXXX800999999999" -> "UNK"
    )

    for ((account, expectedBank) <- accountsToTest) {
        it should s"extract the bank ${expectedBank} from the account ${account}" in {
            val bank = AccountsService.bank(account)
            bank should equal (expectedBank)
        }
    }
}
