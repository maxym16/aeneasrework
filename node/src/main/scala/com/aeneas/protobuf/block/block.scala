package com.aeneas.protobuf

package object block {
  type PBBlock = com.aeneas.protobuf.block.Block
  val PBBlock = com.aeneas.protobuf.block.Block

  type VanillaBlock = com.aeneas.block.Block
  val VanillaBlock = com.aeneas.block.Block

  type PBBlockHeader = com.aeneas.protobuf.block.Block.Header
  val PBBlockHeader = com.aeneas.protobuf.block.Block.Header

  type VanillaBlockHeader = com.aeneas.block.BlockHeader
  val VanillaBlockHeader = com.aeneas.block.BlockHeader

  type PBSignedMicroBlock = com.aeneas.protobuf.block.SignedMicroBlock
  val PBSignedMicroBlock = com.aeneas.protobuf.block.SignedMicroBlock

  type PBMicroBlock = com.aeneas.protobuf.block.MicroBlock
  val PBMicroBlock = com.aeneas.protobuf.block.MicroBlock

  type VanillaMicroBlock = com.aeneas.block.MicroBlock
  val VanillaMicroBlock = com.aeneas.block.MicroBlock
}
