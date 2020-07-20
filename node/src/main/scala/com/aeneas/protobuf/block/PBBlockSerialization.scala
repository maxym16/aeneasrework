package com.aeneas.protobuf.block
import com.google.protobuf.ByteString
import com.aeneas.protobuf.utils.PBUtils

private[block] object PBBlockSerialization {
  def signedBytes(block: PBBlock): Array[Byte] = {
    PBUtils.encodeDeterministic(block)
  }

  def unsignedBytes(block: PBBlock): Array[Byte] = {
    PBUtils.encodeDeterministic(block.withSignature(ByteString.EMPTY))
  }
}
