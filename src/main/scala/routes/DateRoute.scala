package routes

import zio.*
import zio.http.*
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.fn.*

// Timestamp operations and date part extraction.

object DateRoute extends Responses {

  val btcPrice = Model.get[BtcPrice]

  val routes = Routes(
    // Extract date parts (year, day of year) and format timestamp
    Method.GET / "btc" / "hour" -> handler { (req: Request) =>
      sql
        .select(btcPrice)
        .colsJson(t =>
          Seq(
            t.high.round(2),
            t.low.round(2),
            t.open.round(2),
            t.close.round(2),
            t.created.format("DD Mon YYYY HH24:MI") // format timestamp
          )
        )
        .where(t =>
          Seq(
            t.created.year === queryInt(req, "year"), // extract year from timestamp
            t.created.doy === queryInt(req, "doy")    // extract day of year from timestamp
          )
        )
        .orderBy(_.created.asc)
        .run
        .map(jsonListResponse(_))
    },

    // Aggregate data by quarter
    Method.GET / "btc" / "quarter" / "avg" -> handler { (req: Request) =>
      sql
        .select(btcPrice)
        .colsJson(t =>
          Seq(
            Agg.avg(t.close).round(2).as("avg"), // aggregate average
            Agg.max(t.close).round(2).as("max"), // aggregate maximum
            Agg.min(t.close).round(2).as("min")  // aggregate minimum
          )
        )
        .where(t =>
          Seq(
            t.created.year === queryInt(req, "year"),
            t.created.quarter === queryInt(req, "quarter") // extract quarter from timestamp
          )
        )
        .runHead
        .map(jsonObjResponse(_))
    },

    // Extract multiple date parts (year, quarter, week)
    Method.GET / "btc" / "break" -> handler { (req: Request) =>
      sql
        .select(btcPrice)
        .colsJson(t =>
          Seq(
            t.high.round(2).as("price"),     // round to 2 decimal places
            t.created.year.as("year"),       // extract year
            t.created.quarter.as("quarter"), // extract quarter
            t.created.week.as("week"),       // extract week number
            t.created.format("DD Mon YYYY HH24:MI").as("date")
          )
        )
        .where(_.high >= queryBigDecimal(req, "price"))
        .orderBy(_.high.asc)
        .limit(1)
        .runHeadOpt
        .map(jsonOptResponse(_))
    }
  )
}
