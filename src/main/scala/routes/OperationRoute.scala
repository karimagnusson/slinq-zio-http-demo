package routes

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}

// INSERT, UPDATE, DELETE with type-safe request handling.

object OperationRoute extends Responses {

  val trip = Model.get[Trip]
  val city = Model.get[City]

  case class TripInsert(city_id: Int, price: Int)
  object TripInsert {
    given Schema[TripInsert] = DeriveSchema.gen[TripInsert]
  }

  case class TripUpdate(id: Long, price: Int)
  object TripUpdate {
    given Schema[TripUpdate] = DeriveSchema.gen[TripUpdate]
  }

  case class TripDelete(id: Long)
  object TripDelete {
    given Schema[TripDelete] = DeriveSchema.gen[TripDelete]
  }

  val routes = Routes(
    // INSERT with RETURNING
    Method.POST / "insert" / "trip" -> handler { (req: Request) =>
      for {
        data <- req.body.to[TripInsert]
        result <- sql
          .insert(trip)
          .cols(t => (
            t.cityId,
            t.price
          ))
          .values((
            data.city_id,
            data.price
          ))
          .returningJson(t => Seq(
            t.id,
            t.cityId,
            t.price
          ))
          .runHead
      } yield jsonObjResponse(result)
    },

    // UPDATE with RETURNING
    Method.PATCH / "update" / "trip" -> handler { (req: Request) =>
      for {
        data <- req.body.to[TripUpdate]
        result <- sql
          .update(trip)
          .set(_.price ==> data.price)
          .where(_.id === data.id)
          .returningJson(t => Seq(
            t.id,
            t.cityId,
            t.price
          ))
          .runHeadOpt
      } yield jsonOptResponse(result)
    },

    // DELETE with RETURNING
    Method.DELETE / "delete" / "trip" -> handler { (req: Request) =>
      for {
        data <- req.body.to[TripDelete]
        result <- sql
          .delete(trip)
          .where(_.id === data.id)
          .returningJson(t => Seq(
            t.id,
            t.cityId,
            t.price
          ))
          .runHeadOpt
      } yield jsonOptResponse(result)
    }
  )
}