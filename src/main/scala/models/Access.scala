package models

import com.typesafe.config.ConfigFactory
import slinq.pg.zio.api.{*, given}


object Access {

  def getConfig = {

    val conf = ConfigFactory.load()

    PgConfig
      .forDb(conf.getString("db.name"))
      .withUser(conf.getString("db.user"))
      .withPassword(conf.getString("db.pwd"))
  }
}