package routes

import zio.*
import zio.http.*
import zio.json.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.fn.*

// Type-safe queries with case classes using zio-schema for serialization.

object TypeRoute extends Responses {

  val country = Model.get[Country]
  val trip    = Model.get[Trip]

  // JSON codec definitions for types

  case class CountryType(code: String, name: String, population: Int)
  object CountryType {
    given JsonCodec[CountryType] = DeriveJsonCodec.gen[CountryType]
  }

  case class TripType(id: Long, cityId: Int, price: Int)
  object TripType {
    given JsonCodec[TripType] = DeriveJsonCodec.gen[TripType]
  }

  case class TripDataType(cityId: Int, price: Int)
  object TripDataType {
    given JsonCodec[TripDataType] = DeriveJsonCodec.gen[TripDataType]
    given Schema[TripDataType]    = DeriveSchema.gen[TripDataType]
  }

  case class TripPriceType(id: Long, price: Int)
  object TripPriceType {
    given JsonCodec[TripPriceType] = DeriveJsonCodec.gen[TripPriceType]
    given Schema[TripPriceType]    = DeriveSchema.gen[TripPriceType]
  }

  val routes = Routes(
    // SELECT with type-safe result mapping
    Method.GET / "type" / "select" / "country" / string("code") -> handler {
      (code: String, req: Request) =>
        sql
          .select(country)
          .cols(t =>
            (
              t.code,
              t.name,
              t.population
            )
          )
          .where(_.code === code.toUpperCase)
          .runHeadAs[CountryType] // map result to case class
          .map(rsp => Response.json(rsp.toJson))
    },

    // INSERT with type-safe input and output
    Method.POST / "type" / "insert" / "trip" -> handler { (req: Request) =>
      for {
        data <- req.body.to[TripDataType]
        result <- sql
          .insert(trip)
          .cols(t => (t.cityId, t.price))
          .values((data.cityId, data.price))
          .returning(t => (t.id, t.cityId, t.price))
          .runHeadAs[TripType]
      } yield Response.json(result.toJson)
    },

    // UPDATE with type-safe input and output
    Method.PATCH / "type" / "update" / "trip" -> handler { (req: Request) =>
      for {
        data <- req.body.to[TripPriceType] // deserialize JSON to case class
        result <- sql
          .update(trip)
          .set(_.price ==> data.price)
          .where(_.id === data.id)
          .returning(t =>
            (
              t.id,
              t.cityId,
              t.price
            )
          )
          .runHeadAs[TripType] // map result to case class
      } yield Response.json(result.toJson)
    }
  )
}
