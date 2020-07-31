package com.aeneas.account

import java.nio.charset.Charset

import com.aeneas.{EitherMatchers, crypto}
import org.scalatest.{Inside, Matchers, PropSpec}
import com.aeneas.common.utils.{Base58, EitherExt2}
import com.google.common.primitives.Ints
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import scorex.crypto.hash.{Blake2b256, Sha256}

import scala.collection.immutable.TreeSet

/**
 * @author luger. Created on 7/9/20.
 * @version
 */
class AddressTest extends PropSpec with PropertyChecks with Matchers with EitherMatchers with Inside {

    property("Account should get parsed correctly") {
      AddressOrAlias.fromString("Æx4dfqcEt9RrPqGyTfh5cV3Y1Pr4ynwfS3L98jPRKQkeswefWh7n").explicitGet() shouldBe an[Address]
    }

    property("Check seed and addr") {
      val originalSeed = """liguloid dermobranchia xylographer improvidence qasidas intentionalism freedwoman vernissage haden khellin xanthotic gang smooth-sided woodruffs hemeralope limousin pentachromic solutionis zygomycete tilburies electrotonic hoskinston all-affecting attainders"""

      val keys = KeyPair.fromSeed(Base58.encode(("Æ1.0 "+originalSeed).getBytes("UTF-8"))).explicitGet()
      keys.toAddress(Byte.MinValue).stringRepr should equal("Æx3jE9k1AUpqAegvZQm6BBAQ2Nmm142JAzdoZm7SGMRQEkR5ivfp")
      Address.fromString("Æx3jE9k1AUpqAegvZQm6BBAQ2Nmm142JAzdoZm7SGMRQEkR5ivfp").explicitGet().toString should equal("Æx3jE9k1AUpqAegvZQm6BBAQ2Nmm142JAzdoZm7SGMRQEkR5ivfp")
      Address.fromPublicKey(keys.publicKey, Byte.MinValue).toString should equal("Æx3jE9k1AUpqAegvZQm6BBAQ2Nmm142JAzdoZm7SGMRQEkR5ivfp")
      val a = Address.fromPublicKey(KeyPair.fromSeed(Base58.encode((originalSeed).getBytes("UTF-8"))).explicitGet().publicKey).toString
      println (s"a : $a")
      a shouldBe an[String]
    }

}
