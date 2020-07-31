package com.aeneas.it.sync.grpc

import com.google.protobuf.ByteString
import com.aeneas.account.{Address, KeyPair}
import com.aeneas.common.state.ByteStr
import com.aeneas.common.utils.EitherExt2
import com.aeneas.it.api.SyncGrpcApi._
import com.aeneas.it.sync._
import com.aeneas.it.sync.smartcontract.invokeScrTxSupportedVersions
import com.aeneas.it.util._
import com.aeneas.lang.v1.FunctionHeader
import com.aeneas.lang.v1.compiler.Terms.{CONST_BYTESTR, FUNCTION_CALL}
import com.aeneas.lang.v1.estimator.v2.ScriptEstimatorV2
import com.aeneas.lang.v1.estimator.v3.ScriptEstimatorV3
import com.wavesplatform.protobuf.transaction.DataTransactionData.DataEntry
import com.wavesplatform.protobuf.transaction.{PBTransactions, PBRecipients, Recipient}
import com.aeneas.transaction.TxVersion
import com.aeneas.transaction.smart.script.ScriptCompiler
import io.grpc.Status.Code

class InvokeScriptTransactionGrpcSuite extends GrpcBaseTransactionSuite {
  private val (firstContract, firstContractAddr)   = (firstAcc, firstAddress)
  private val (secondContract, secondContractAddr) = (secondAcc, secondAddress)
  private val thirdContract                        = KeyPair("thirdContract".getBytes("UTF-8"))
  private val thirdContractAddr                    = PBRecipients.create(Address.fromPublicKey(thirdContract.publicKey)).getPublicKeyHash
  private val caller                               = thirdAcc

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    val scriptText =
      """
        |{-# STDLIB_VERSION 3 #-}
        |{-# CONTENT_TYPE DAPP #-}
        |
        | @Callable(inv)
        | func foo(a:ByteVector) = {
        |  WriteSet([DataEntry("a", a), DataEntry("sender", inv.caller.bytes)])
        | }
        | @Callable(inv)
        | func emptyKey() = {
        |  WriteSet([DataEntry("", "a")])
        | }
        |
        | @Callable(inv)
        | func default() = {
        |  WriteSet([DataEntry("a", "b"), DataEntry("sender", "senderId")])
        | }
        |
        | @Verifier(tx)
        | func verify() = {
        |    match tx {
        |        case _: TransferTransaction => false
        |        case _ => true
        | }
        |}
        |
        """.stripMargin
    val scriptTextV4 =
      """
        |{-# STDLIB_VERSION 4 #-}
        |{-# CONTENT_TYPE DAPP #-}
        |
        | @Callable(inv)
        |func foo() = [IntegerEntry("", 1)]
        |
        | @Callable(inv)
        |func bar() = [IntegerEntry("", 2)]
        |
        """.stripMargin
    val script  = ScriptCompiler.compile(scriptText, ScriptEstimatorV2).explicitGet()._1
    val script2 = ScriptCompiler.compile(scriptTextV4, ScriptEstimatorV3).explicitGet()._1
    sender.broadcastTransfer(firstAcc, Recipient().withPublicKeyHash(thirdContractAddr), 10.waves, minFee, waitForTx = true)
    sender.setScript(firstContract, Right(Some(script)), setScriptFee, waitForTx = true)
    sender.setScript(secondContract, Right(Some(script)), setScriptFee, waitForTx = true)
    sender.setScript(thirdContract, Right(Some(script2)), setScriptFee, waitForTx = true)

    val scriptInfo = sender.scriptInfo(firstAddress)
    PBTransactions.toVanillaScript(scriptInfo.scriptBytes) shouldBe Some(script)
  }

  test("dApp caller invokes a function on a dApp") {
    val arg = ByteStr(Array(42: Byte))
    for (v <- invokeScrTxSupportedVersions) {
      val contract = if (v < 2) firstContractAddr else secondContractAddr
      sender.broadcastInvokeScript(
        caller,
        Recipient().withPublicKeyHash(contract),
        Some(FUNCTION_CALL(FunctionHeader.User("foo"), List(CONST_BYTESTR(arg).explicitGet()))),
        fee = 1.waves,
        waitForTx = true
      )

      sender.getDataByKey(contract, "a") shouldBe List(DataEntry("a", DataEntry.Value.BinaryValue(ByteString.copyFrom(arg.arr))))
      sender.getDataByKey(contract, "sender") shouldBe List(
        DataEntry("sender", DataEntry.Value.BinaryValue(ByteString.copyFrom(caller.toAddress.bytes)))
      )
    }
  }

  test("dApp caller invokes a default function on a dApp") {
    for (v <- invokeScrTxSupportedVersions) {
      val contract = if (v < 2) firstContractAddr else secondContractAddr
      sender.broadcastInvokeScript(
        caller,
        Recipient().withPublicKeyHash(contract),
        functionCall = None,
        fee = 1.waves,
        waitForTx = true
      )
      sender.getDataByKey(contract, "a") shouldBe List(DataEntry("a", DataEntry.Value.StringValue("b")))
      sender.getDataByKey(contract, "sender") shouldBe List(DataEntry("sender", DataEntry.Value.StringValue("senderId")))
    }
  }

  test("verifier works") {
    for (v <- invokeScrTxSupportedVersions) {
      val contract    = if (v < 2) firstContractAddr else secondContractAddr
      val dAppBalance = sender.wavesBalance(contract)
      assertGrpcError(
        sender.broadcastTransfer(firstAcc, Recipient().withPublicKeyHash(contract), transferAmount, 1.waves),
        "Transaction is not allowed by account-script",
        Code.INVALID_ARGUMENT
      )
      sender.wavesBalance(contract) shouldBe dAppBalance
    }
  }

  test("not able to set an empty key by InvokeScriptTransaction with version >= 2") {
    val tx1 = sender.broadcastInvokeScript(
      caller,
      Recipient().withPublicKeyHash(secondContractAddr),
      Some(FUNCTION_CALL(FunctionHeader.User("emptyKey"), List.empty)),
      fee = 1.waves,
      version = TxVersion.V2,
      waitForTx = true
    )

    sender.stateChanges(tx1.id)._2.error.get.text should include("Empty keys aren't allowed in tx version >= 2")

    val tx2 = sender.broadcastInvokeScript(
      caller,
      Recipient().withPublicKeyHash(thirdContractAddr),
      Some(FUNCTION_CALL(FunctionHeader.User("foo"), List.empty)),
      fee = 1.waves,
      version = TxVersion.V2,
      waitForTx = true
    )

    sender.stateChanges(tx2.id)._2.error.get.text should include("Empty keys aren't allowed in tx version >= 2")
  }
}
