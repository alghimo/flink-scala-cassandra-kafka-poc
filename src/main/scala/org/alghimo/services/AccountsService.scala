package org.alghimo.services

/**
  * Created by alghimo on 9/1/2016.
  */
object AccountsService extends java.io.Serializable {
    private final val UNKNOWN_BANK = "UNK"

    /**
      * @todo Get this from a DB
      */
    private final val bankCodes = Map[Short, String](
        300.toShort -> "ING",
        400.toShort -> "BNP",
        500.toShort -> "BEL",
        600.toShort -> "KBC",
        700.toShort -> "ARG"
    )

    /**
      * Extract the country from an account.
      * @param account Source account to calculate the country.
      * @return
      */
    def country(account: String): String = account.substring(0, 2)

    /**
      * Extract the bank an account belongs to.
      * @param account Source account to calculate the bank.
      * @return
      */
    def bank(account: String): String = bankCodes.getOrElse(account.substring(4, 7).toShort, UNKNOWN_BANK)
}

