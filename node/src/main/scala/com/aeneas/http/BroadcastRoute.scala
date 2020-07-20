package com.aeneas.http

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Route
import cats.syntax.either._
import com.aeneas.api.http.{ApiError, ApiRoute, jsonParammedPost}
import com.aeneas.lang.ValidationError
import com.aeneas.network._
import com.aeneas.transaction.Transaction
import play.api.libs.json._

trait BroadcastRoute { _: ApiRoute =>
  def utxPoolSynchronizer: UtxPoolSynchronizer

  def broadcast[A: Reads](f: A => Either[ValidationError, Transaction]): Route = jsonParammedPost[A] { (a, params) =>
    f(a).fold[ToResponseMarshallable](
      ApiError.fromValidationError,
      tx => {
        val p = utxPoolSynchronizer.publish(tx)
        p.transformE(r => r.bimap(ApiError.fromValidationError, t => tx.json() ++ params.get("trace").fold(Json.obj())(_ => Json.obj("trace" -> p.trace.map(_.loggedJson)))))
      }
    )
  }
}
