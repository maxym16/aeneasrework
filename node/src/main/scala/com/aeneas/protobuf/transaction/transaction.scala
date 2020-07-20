package com.aeneas.protobuf

package object transaction {
  type PBOrder = com.aeneas.protobuf.order.Order
  val PBOrder = com.aeneas.protobuf.order.Order

  type VanillaOrder = com.aeneas.transaction.assets.exchange.Order
  val VanillaOrder = com.aeneas.transaction.assets.exchange.Order

  type PBTransaction = com.aeneas.protobuf.transaction.Transaction
  val PBTransaction = com.aeneas.protobuf.transaction.Transaction

  type PBSignedTransaction = com.aeneas.protobuf.transaction.SignedTransaction
  val PBSignedTransaction = com.aeneas.protobuf.transaction.SignedTransaction

  type VanillaTransaction = com.aeneas.transaction.Transaction
  val VanillaTransaction = com.aeneas.transaction.Transaction

  type VanillaSignedTransaction = com.aeneas.transaction.SignedTransaction

  type VanillaAssetId = com.aeneas.transaction.Asset
}
