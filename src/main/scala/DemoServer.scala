import zio.*
import zio.http.*
import slinq.pg.zio.api.{*, given}
import routes.Routes
import models.Access

object DemoServer extends ZIOAppDefault {

  val dbConfig    = Access.getConfig
  val dbLayer     = SlinqPg.layer(dbConfig)
  val configLayer = Server.defaultWithPort(9000)

  def run =
    Server.serve(Routes.app)
      .provide(configLayer ++ dbLayer)
      .onInterrupt(ZIO.logInfo("Server interrupted, shutting down..."))
}
