package com.aeneas.it.sync.smartcontract

import com.typesafe.config.Config
import com.aeneas.api.http.ApiError.ScriptCompilerError
import com.aeneas.common.state.ByteStr
import com.aeneas.common.utils.EitherExt2
import com.aeneas.it.NodeConfigs
import com.aeneas.it.NodeConfigs.Default
import com.aeneas.it.api.SyncHttpApi._
import com.aeneas.it.api.TransactionInfo
import com.aeneas.it.sync._
import com.aeneas.it.transactions.BaseTransactionSuite
import com.aeneas.lang.v1.estimator.v2.ScriptEstimatorV2
import com.aeneas.transaction.Asset.IssuedAsset
import com.aeneas.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class RideUpdateAssetInfoTxSuite extends BaseTransactionSuite with CancelAfterFailure {

  override protected def nodeConfigs: Seq[Config] =
    NodeConfigs.Builder(Default, 1, Seq.empty)
      .overrideBase(_.quorum(0))
      .overrideBase(_.minAssetInfoUpdateInterval(1))
      .buildNonConflicting()

  private val dApp = firstAddress
  private val smartAcc = secondAddress

  private var asset1: IssuedAsset = _
  private var asset2: IssuedAsset = _

  private val name: String = "MyAsset"
  private val description: String = "Some description"
  private val fee = issueFee + smartFee * 2
  private val timestamp = System.currentTimeMillis()

  def sourceDApp(version: Int): String =
    s"""
       |{-# STDLIB_VERSION $version #-}
       |{-# CONTENT_TYPE DAPP #-}
       |{-# SCRIPT_TYPE ACCOUNT #-}
       |
       |@Verifier(tx)
       |func verify() = {
       |  match tx {
       |    case uai: UpdateAssetInfoTransaction =>
       |      uai.id.size() == 32
       |      && uai.version == 1
       |      && uai.sender == this
       |      && uai.senderPublicKey == base58'${pkByAddress(firstAddress).publicKey.toString}'
       |      && uai.name == "$name"
       |      && uai.description == "$description"
       |      && uai.fee == $fee
       |      && uai.timestamp == $timestamp
       |      && sigVerify(uai.bodyBytes, uai.proofs[0], uai.senderPublicKey)
       |    case _ => throw("UpdateAssetInfoTx was not matched")
       |  }
       |}
        """.stripMargin

  def sourceAcc(version: Int): String =
    s"""
       |{-# STDLIB_VERSION $version #-}
       |{-# CONTENT_TYPE EXPRESSION #-}
       |{-# SCRIPT_TYPE ACCOUNT #-}
       |
       |match tx {
       |  case uai: UpdateAssetInfoTransaction =>
       |    uai.id.size() == 32
       |    && uai.version == 1
       |    && uai.sender == this
       |    && uai.senderPublicKey == base58'${pkByAddress(secondAddress).publicKey.toString}'
       |    && uai.name == "$name"
       |    && uai.description == "$description"
       |    && uai.fee == $fee
       |    && uai.timestamp == $timestamp
       |    && sigVerify(uai.bodyBytes, uai.proofs[0], uai.senderPublicKey)
       |  case _ => throw("UpdateAssetInfoTx was not matched")
       |}""".stripMargin

  def sourceAsset(version: Int): String =
    s"""
       |{-# STDLIB_VERSION $version #-}
       |{-# CONTENT_TYPE EXPRESSION #-}
       |{-# SCRIPT_TYPE ASSET #-}
       |
       |match tx {
       |  case uai: UpdateAssetInfoTransaction =>
       |    uai.id.size() == 32
       |    && uai.version == 1
       |    && uai.sender == this.issuer
       |    && uai.senderPublicKey == this.issuerPublicKey
       |    && uai.name == "$name"
       |    && uai.description == "$description"
       |    && uai.fee == $fee
       |    && uai.timestamp == $timestamp
       |  case _ => throw("UpdateAssetInfoTx was not matched")
       |}""".stripMargin

  test("can't compile V3 with UpdateAssetInfoTransaction") {
    Seq(sourceDApp(3), sourceAcc(3), sourceAsset(3))
      .foreach { scriptV3 =>
        assertApiError(sender.scriptCompile(scriptV3)) { error =>
          error.statusCode shouldBe 400
          error.id shouldBe ScriptCompilerError.Id
          error.message should include("Undefined type: `UpdateAssetInfoTransaction`")
        }
      }
  }

  test("can issue assets and set contracts with check of UpdateAssetInfoTransaction") {
    val scriptDApp = ScriptCompiler.compile(sourceDApp(4), ScriptEstimatorV2).explicitGet()._1.bytes().base64
    val scriptAcc = ScriptCompiler.compile(sourceAcc(4), ScriptEstimatorV2).explicitGet()._1.bytes().base64
    val scriptAsset = ScriptCompiler.compile(sourceAsset(4), ScriptEstimatorV2).explicitGet()._1.bytes().base64

    val issue1 = sender.issue(dApp, script = Some(scriptAsset), waitForTx = true)
    val issue2 = sender.issue(smartAcc, script = Some(scriptAsset), waitForTx = true)
    asset1 = IssuedAsset(ByteStr.decodeBase58(issue1.id).get)
    asset2 = IssuedAsset(ByteStr.decodeBase58(issue2.id).get)

    sender.setScript(dApp, Some(scriptDApp), waitForTx = true)
    sender.setScript(smartAcc, Some(scriptAcc), waitForTx = true)
  }

  test("can check UpdateAssetInfo tx from contracts") {
    val asset2Height = sender.transactionInfo[TransactionInfo](asset2.id.toString).height
    nodes.waitForHeight(asset2Height + 2)

    sender.updateAssetInfo(pkByAddress(dApp), asset1.id.toString, name, description, fee, timestamp = Some(timestamp), waitForTx = true)
    sender.updateAssetInfo(pkByAddress(smartAcc), asset2.id.toString, name, description, fee, timestamp = Some(timestamp), waitForTx = true)
  }

}


