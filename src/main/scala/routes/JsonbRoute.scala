package routes

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.fn.*
import slinq.pg.column.TypeCol

// JSONB operations with type-safe request handling.
// JSON operators follow PostgreSQL syntax: -> for object, ->> for text, #>> for deep path.

object JsonbRoute extends Responses {

  val countryData = Model.get[CountryData]

  case class PhoneData(code: String, phone: String)
  object PhoneData {
    given Schema[PhoneData] = DeriveSchema.gen[PhoneData]
  }

  case class CodeData(code: String)
  object CodeData {
    given Schema[CodeData] = DeriveSchema.gen[CodeData]
  }

  val routes = Routes(
    // Select JSONB field
    Method.GET / "jsonb" / "country" / string("code") -> handler { (code: String, req: Request) =>
      sql
        .select(countryData)
        .colsJson(t =>
          Seq(
            t.uid,
            t.code,
            t.langs, // array field
            t.data   // jsonb field
          )
        )
        .where(_.code === code.toUpperCase)
        .runHeadOpt
        .map(jsonOptResponse(_))
    },

    // Query nested JSONB field and concatenate JSONB objects
    Method.GET / "jsonb" / "capital" / string("name") -> handler { (name: String, req: Request) =>
      sql
        .select(countryData)
        .colsJson(t =>
          Seq(
            t.uid,
            t.code,
            t.langs,
            (t.data || t.cities).as("data") // concatenate JSONB objects
          )
        )
        .where(_.data -> "capital" ->> "name" === name) // query nested field
        .runHeadOpt
        .map(jsonOptResponse(_))
    },

    // Access JSONB array element and extract nested values
    Method.GET / "jsonb" / "city" / "population" -> handler { (req: Request) =>
      sql
        .select(countryData)
        .colsJson(t =>
          Seq(
            t.uid,
            t.code,
            (t.data ->> "name").as("name"), // extract text value
            (t.cities -> "cities" -> 0).as("largest_city") // access array element
          )
        )
        .where(t => (t.cities -> "cities" -> 0 ->> "population").isNotNull)
        .orderBy(t => (t.cities -> "cities" -> 0 ->> "population").asInt.desc)
        .limit(5)
        .run
        .map(jsonListResponse)
    },

    // Deep path extraction and aggregate
    Method.GET / "jsonb" / "capital-avg" / string("cont") -> handler {
      (cont: String, req: Request) =>
        sql
          .select(countryData)
          .colsJson(t =>
            Seq(
              Agg.avg((t.data #>> Seq("capital", "population")).asInt) // deep path extraction
            )
          )
          .where(t =>
            Seq(
              (t.data #>> Seq("capital", "population")).isNotNull,
              t.data ->> "continent" === cont
            )
          )
          .runHead
          .map(jsonObjResponse)
    },

    // Add field to JSONB object
    Method.PATCH / "jsonb" / "add" / "phone" -> handler { (req: Request) =>
      for {
        data <- req.body.to[PhoneData]
        result <- sql
          .update(countryData)
          .set(_.data += Jsonb(s"""{"phone": "${data.phone}"}""")) // add field to JSONB object
          .where(_.code === data.code)
          .returning(_.data)
          .runHeadOpt
      } yield jsonOptResponse(result)
    },

    // Remove field from JSONB object
    Method.PATCH / "jsonb" / "del" / "phone" -> handler { (req: Request) =>
      for {
        data <- req.body.to[CodeData]
        result <- sql
          .update(countryData)
          .set(_.data -= "phone") // remove field from JSONB object
          .where(_.code === data.code)
          .returning(_.data)
          .runHeadOpt
      } yield jsonOptResponse(result)
    }
  )
}
