package com.aeneas.transaction.serialization.impl

import cats.implicits._
import com.wavesplatform.protobuf.transaction.{PBTransactions, SignedTransaction => PBSignedTransaction}
import com.wavesplatform.protobuf.utils.PBUtils
import com.aeneas.transaction.{PBParsingError, Transaction}

import scala.util.Try

object PBTransactionSerializer {
  def bodyBytes(tx: Transaction): Array[Byte] =
    PBUtils.encodeDeterministic(PBTransactions.protobuf(tx).getTransaction)

  def bytes(tx: Transaction): Array[Byte] =
    PBUtils.encodeDeterministic(PBTransactions.protobuf(tx))

  def parseBytes(bytes: Array[Byte]): Try[Transaction] =
    PBSignedTransaction
      .validate(bytes)
      .adaptErr { case err => PBParsingError(err) }
      .flatMap(PBTransactions.tryToVanilla)
}
