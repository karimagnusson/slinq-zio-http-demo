package routes

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import zio.*
import zio.stream.ZStream
import zio.http.*
import scala.language.implicitConversions
import slinq.pg.zio.api.Jsonb

import java.io.{CharArrayWriter, PrintWriter}

object Routes {

  private val routes = SelectRoute.routes ++
    OperationRoute.routes ++
    CacheRoute.routes ++
    StreamRoute.routes ++
    JsonbRoute.routes ++
    ArrayRoute.routes ++
    DateRoute.routes ++
    TypeRoute.routes

  val app = (routes).mapError { ex =>
    Response.json(
      """{"error": "%s"}""".format(ex.getMessage)
    )
  }
}

trait Responses {

  def queryString(req: Request, key: String): String =
    req.url.queryParams.map(key)(0)

  def queryStringOpt(req: Request, key: String): Option[String] =
    req.url.queryParams.map.get(key).map(_(0))

  def queryInt(req: Request, key: String): Int =
    req.url.queryParams.map(key)(0).toInt

  def queryIntOpt(req: Request, key: String): Option[Int] =
    req.url.queryParams.map.get(key).map(_(0).toInt)

  def queryBigDecimal(req: Request, key: String): BigDecimal =
    BigDecimal(req.url.queryParams.map(key)(0))

  def queryBigDecimalOpt(req: Request, key: String): Option[BigDecimal] =
    req.url.queryParams.map.get(key).map(v => BigDecimal(v(0)))

  val notFound = """{"message": "not found"}"""
  val okTrue   = """{"ok": true}"""

  val jsonObjResponse: Jsonb => Response = { obj =>
    Response.json(obj.value)
  }

  val jsonOptResponse: Option[Jsonb] => Response = {
    case Some(obj) => Response.json(obj.value)
    case None      => Response.json(notFound)
  }

  val jsonListResponse: List[Jsonb] => Response = { list =>
    Response.json("[%s]".format(list.map(_.value).mkString(",")))
  }

  val jsonOkResponse: Unit => Response = _ => jsonObjResponse(Jsonb(okTrue))
}
