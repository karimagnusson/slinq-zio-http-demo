package routes

import java.util.UUID
import zio.*
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.http.*
import zio.http.codec.TextBinaryCodec.fromSchema
import scala.language.implicitConversions
import java.sql.Timestamp
import models.*
import slinq.pg.zio.api.{*, given}
import slinq.pg.fn.*

// Streaming data export and import.

object StreamRoute extends Responses {

  val coinPrice     = Model.get[CoinPrice]
  val tempCoinPrice = Model.get[TempCoinPrice]

  val headers = Headers(
    Header.ContentType(MediaType.text.csv),
    Header.ContentDisposition.Attachment(Some("coins.csv"))
  )

  val makeLine: Tuple3[String, String, Timestamp] => String = { case (coin, price, takenAt) =>
    "%s,%s,%s".format(coin, price, takenAt.toString)
  }

  val parseLine: String => Tuple3[String, BigDecimal, Timestamp] = { line =>
    line.split(',') match {
      case Array(coin, price, takenAt) =>
        (coin, BigDecimal(price), Timestamp.valueOf(takenAt))
      case _ =>
        throw new Exception("invalid file")
    }
  }

  val insertCoinPriceStm = sql
    .insert(coinPrice)
    .cols(t =>
      (
        t.coin,
        t.price,
        t.created
      )
    )
    .cache

  val routes = Routes(
    // Stream database query results as CSV file (import data first using /stream/import)
    Method.GET / "stream" / "export" / string("coin") -> handler { (coin: String, req: Request) =>
      for {
        env <- ZIO.environment[SlinqPg]

        stream = sql
          .select(coinPrice)
          .cols(t =>
            (
              t.coin,
              Fn.roundStr(t.price, 2),
              t.created
            )
          )
          .where(_.coin === coin.toUpperCase)
          .orderBy(_.created.asc)
          .stream
          .map(makeLine)
          .intersperse("\n")
          .provideEnvironment(env)

        response = Response(
          status = Status.Ok,
          headers = headers,
          body = Body.fromStream(stream)
        )
      } yield response
    },

    // Stream CSV file upload directly to database with batching (sample file in csv folder)
    Method.POST / "stream" / "import" -> handler { (req: Request) =>
      req.body.asStream
        .via(ZPipeline.utf8Decode)
        .via(ZPipeline.splitLines)
        .map(parseLine)
        .run(insertCoinPriceStm.asSink)  // insert to database
        .as(jsonOkResponse(()))
    },

    // Safe import using temporary table with rollback on failure (sample file in csv folder)
    Method.POST / "stream" / "import" / "safe" -> handler { (req: Request) =>
      val uid = UUID.randomUUID

      (for {

        _ <- req // stream to temporary table
          .body.asStream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .map(parseLine)
          .map { // add UUID for temp table isolation
            case (coin, price, takenAt) =>
              (uid, coin, price, takenAt)
          }
          .run(
            sql
              .insert(tempCoinPrice)
              .cols(t =>
                (
                  t.uid,
                  t.coin,
                  t.price,
                  t.created
                )
              )
              .cache
              .asSink
          )

        _ <- sql // move data using INSERT from SELECT
          .insert(coinPrice)
          .cols(t =>
            (
              t.coin,
              t.price,
              t.created
            )
          )
          .fromSelect(
            sql
              .select(tempCoinPrice)
              .cols(t =>
                (
                  t.coin,
                  t.price,
                  t.created
                )
              )
              .where(_.uid === uid)
          )
          .run

      } yield Response.json(okTrue)).tapEither { _ =>
        sql // cleanup temp table on success or failure
          .delete(tempCoinPrice)
          .where(_.uid === uid)
          .run
      }
    }
  )
}
