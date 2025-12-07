package routes

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.column.TypeCol

// PostgreSQL array operations with type-safe request handling.

object ArrayRoute extends Responses {

  val countryData = Model.get[CountryData]

  case class LangData(code: String, lang: String)
  object LangData {
    given Schema[LangData] = DeriveSchema.gen[LangData]
  }

  val routes = Routes(
    // Get country with its languages array as JSON
    Method.GET / "array" / "langs" / string("code") -> handler { (code: String, req: Request) =>
      sql
        .select(countryData)
        .colsJson(t =>
          Seq(
            t.code,
            t.langs
          )
        )
        .where(_.code === code.toUpperCase)
        .runHeadOpt
        .map(jsonOptResponse(_))
    },

    // Add language to array (unique + sorted ascending)
    Method.PATCH / "array" / "add" / "lang" -> handler { (req: Request) =>
      for {
        data <- req.body.to[LangData]
        result <- sql
          .update(countryData)
          .set(_.langs.addUniqueAsc(data.lang)) // addUniqueAsc ensures uniqueness and sorts ascending
          .where(_.code === data.code)
          .returningJson(t =>
            Seq(
              t.code,
              t.langs
            )
          )
          .runHeadOpt
      } yield jsonOptResponse(result)
    },

    // Remove all instances of a language from array
    Method.PATCH / "array" / "del" / "lang" -> handler { (req: Request) =>
      for {
        data <- req.body.to[LangData]
        result <- sql
          .update(countryData)
          .set(_.langs -= data.lang) // -= removes all occurrences
          .where(_.code === data.code)
          .returningJson(t =>
            Seq(
              t.code,
              t.langs
            )
          )
          .runHeadOpt
      } yield jsonOptResponse(result)
    }
  )
}
