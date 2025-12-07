package routes

import zio.*
import zio.http.*
import zio.http.codec.PathCodec.path
import scala.language.implicitConversions
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.fn.*

// SELECT queries with JOIN, subqueries, and aggregates.

object SelectRoute extends Responses {

  val city     = Model.get[City]
  val country  = Model.get[Country]
  val language = Model.get[Language]

  val routes = Routes(
    // Simple SELECT
    Method.GET / "select" / "country" / string("code") -> handler { (code: String, req: Request) =>
      sql
        .select(country)
        .colsJson(t =>
          Seq(
            t.code,
            t.name,
            t.continent,
            t.region
          )
        )
        .where(_.code === code.toUpperCase)
        // .printSql  // uncomment to print the SQL query
        .runHeadOpt
        .map(jsonOptResponse(_))
    },

    // JOIN with custom field names
    Method.GET / "select" / "cities" / string("code") -> handler { (code: String, req: Request) =>
      sql
        .select(city, country)
        .colsJson(t =>
          Seq(
            t.a.code,
            t.a.population,
            t.a.name.as("city_name"),    // custom field name
            t.b.name.as("country_name"), // custom field name
            t.b.continent,
            t.b.region
          )
        )
        .joinOn(_.code, _.code)
        .where(_.b.code === code.toUpperCase)
        .orderBy(_.a.population.desc)
        .limit(5)
        // .printSql  // uncomment to print the SQL query
        .run
        .map(jsonListResponse(_))
    },

    // Subquery as nested object
    Method.GET / "select" / "lang" / string("code") -> handler { (code: String, req: Request) =>
      sql
        .select(country)
        .colsJson(t =>
          Seq(
            t.code,
            t.name,
            sql // subquery returns single object
              .select(language)
              .colsJson(s =>
                Seq(
                  s.name,
                  s.percentage
                )
              )
              .where(s =>
                Seq(
                  s.code <=> t.code,
                  s.isOfficial === true
                )
              )
              .limit(1)
              .asColumn
              .first
              .as("language")
          )
        )
        .where(_.code === code.toUpperCase)
        // .printSql // uncomment to print the SQL query
        .runHeadOpt
        .map(jsonOptResponse(_))
    },

    // Nested object with Fn.json and subquery as array
    Method.GET / "select" / "country-cities" / string("code") -> handler {
      (code: String, req: Request) =>
        sql
          .select(country)
          .colsJson(t =>
            Seq(
              t.code,
              t.name,
              Fn.json(
                Seq( // group columns into nested object
                  t.continent,
                  t.region,
                  t.population
                )
              ).as("info"),
              sql // subquery returns array of objects
                .select(city)
                .colsJson(s =>
                  Seq(
                    s.name,
                    s.population
                  )
                )
                .where(_.code <=> t.code)
                .orderBy(_.population.desc)
                .limit(5)
                .asColumn
                .as("cities")
            )
          )
          .where(_.code === code.toUpperCase)
          // .printSql  // uncomment to print the SQL query
          .runHeadOpt
          .map(jsonOptResponse(_))
    },

    // Optional WHERE conditions
    Method.GET / "select" / "optional" -> handler { (req: Request) =>
      sql
        .select(country)
        .colsJson(t =>
          Seq(
            t.code,
            t.name,
            t.continent,
            t.region,
            t.population
          )
        )
        .whereOpt(t =>
          Seq( // only applies conditions with Some values
            t.continent === queryStringOpt(req, "cont"),
            t.region === queryStringOpt(req, "region"),
            t.population > queryIntOpt(req, "pop_gt"),
            t.population < queryIntOpt(req, "pop_lt")
          )
        )
        .orderBy(_.name.asc)
        .limit(10)
        // .printSql  // uncomment to print the SQL query
        .run
        .map(jsonListResponse(_))
    },

    // Complex WHERE with AND/OR logic
    Method.GET / "select" / "and-or" / string("cont") -> handler { (cont: String, req: Request) =>
      sql
        .select(country)
        .colsJson(t =>
          Seq(
            t.code,
            t.name,
            t.continent,
            t.region,
            t.population,
            t.surfaceArea,
            t.lifeExpectancy,
            t.gnp
          )
        )
        .where(t =>
          Seq(
            t.continent === cont,
            Or( // combine conditions with OR
              And(
                t.population > 20000000,
                t.surfaceArea > 500000
              ),
              And(
                t.lifeExpectancy > 65,
                t.gnp > 150000
              )
            )
          )
        )
        .orderBy(t =>
          Seq(
            t.population.desc,
            t.lifeExpectancy.desc
          )
        )
        .limit(10)
        // .printSql  // uncomment to print the SQL query
        .run
        .map(jsonListResponse(_))
    },

    // Aggregate functions
    Method.GET / "select" / "population" / string("cont") -> handler {
      (cont: String, req: Request) =>
        sql
          .select(country)
          .colsJson(t =>
            Seq(
              Count.all.as("count"),
              Agg.avg(t.population).as("avg"),
              Agg.max(t.population).as("max"),
              Agg.min(t.population).as("min")
            )
          )
          .where(_.continent === cont)
          // .printSql  // uncomment to print the SQL query
          .runHead
          .map(jsonObjResponse(_))
    }
  )
}
