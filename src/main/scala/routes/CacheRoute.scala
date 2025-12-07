package routes

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}

// Cached queries with type-safe request handling.

object CacheRoute extends Responses {

  val trip    = Model.get[Trip]
  val city    = Model.get[City]
  val country = Model.get[Country]

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

  // Cached SELECT with pickWhere for runtime arguments
  val selectCountryStm = sql
    .select(country)
    .colsJson(t =>
      Seq(
        t.code,
        t.name,
        t.continent,
        t.region
      )
    )
    .all                            // no WHERE conditions before pickWhere
    .pickWhere(_.code.use === Arg) // WHERE clause with runtime argument
    .cache

  // Cached JOIN with multiple pickWhere arguments
  val selectJoinStm = sql
    .select(city, country)
    .colsJson(t =>
      Seq(
        t.a.code,
        t.a.population,
        t.a.name.as("city_name"),    // custom field name instead of default "name"
        t.b.name.as("country_name"), // custom field name instead of default "name"
        t.b.gnp,
        t.b.continent,
        t.b.region
      )
    )
    .joinOn(_.code, _.code)
    .where(t =>
      Seq(
        t.b.continent === "Asia",
        t.b.gnp.isNotNull
      )
    )
    .orderBy(_.a.population.desc)
    .limit(5)
    .pickWhere(t =>
      (
        t.b.population.use >= Arg,
        t.b.gnp.use >= Arg
      )
    )
    .cache

  // Cached INSERT
  val insertTripStm = sql
    .insert(trip)
    .cols(t =>
      (
        t.cityId,
        t.price
      )
    )
    .returningJson(t =>
      Seq(
        t.id,
        t.cityId,
        t.price
      )
    )
    .cache

  // Cached UPDATE with pickSet and pickWhere
  val updateTripStm = sql
    .update(trip)
    .pickSet(_.price.use ==> Arg)
    .pickWhere(_.id.use === Arg)
    .returningJson(t =>
      Seq(
        t.id,
        t.cityId,
        t.price
      )
    )
    .cache

  // Cached DELETE
  val deleteTripStm = sql
    .delete(trip)
    .pickWhere(_.id.use === Arg)
    .returningJson(t =>
      Seq(
        t.id,
        t.cityId,
        t.price
      )
    )
    .cache

  val routes = Routes(
    // Run cached SELECT with single argument
    Method.GET / "cache" / "select" / "country" / string("code") -> handler {
      (code: String, req: Request) =>
        selectCountryStm
          .runHeadOpt(code.toUpperCase)
          .map(jsonOptResponse(_))
    },

    // Run cached JOIN with multiple arguments
    Method.GET / "cache" / "join" / int("pop") / string("gnp") -> handler {
      (pop: Int, gnp: String, req: Request) =>
        selectJoinStm
          .run(pop, BigDecimal(gnp))
          .map(jsonListResponse(_))
    },

    // Run cached INSERT
    Method.POST / "cache" / "insert" / "trip" -> handler { (req: Request) =>
      for {
        data   <- req.body.to[TripInsert]
        result <- insertTripStm.runHead((data.city_id, data.price))
      } yield jsonObjResponse(result)
    },

    // Run cached UPDATE
    Method.PATCH / "cache" / "update" / "trip" -> handler { (req: Request) =>
      for {
        data   <- req.body.to[TripUpdate]
        result <- updateTripStm.runHeadOpt(data.price, data.id)
      } yield jsonOptResponse(result)
    },

    // Run cached DELETE
    Method.DELETE / "cache" / "delete" / "trip" -> handler { (req: Request) =>
      for {
        data   <- req.body.to[TripDelete]
        result <- deleteTripStm.runHeadOpt(data.id)
      } yield jsonOptResponse(result)
    }
  )
}
