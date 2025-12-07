import java.util.UUID
import java.sql.Timestamp
import slinq.pg.zio.api.{*, given}


package object models {

  class CountryData extends Model("country_data") {
    val uid = column[UUID]("uid")
    val code = column[String]("code")
    val langs = column[Seq[String]]("langs")
    val data = column[Jsonb]("data")
    val cities = column[Jsonb]("cities")
  }

  Model.register[CountryData]

  class City extends Model("city") {
    val id = column[Long]("id")
    val code = column[String]("code")
    val name = column[String]("name")
    val district = column[String]("district")
    val population = column[Int]("population")
  }

  Model.register[City]

  class Country extends Model("country") {
    val id = column[Long]("id")
    val code = column[String]("code")
    val name = column[String]("name")
    val continent = column[String]("continent")
    val region = column[String]("region")
    val surfaceArea = column[Float]("surface_area")
    val indepYear = column[Short]("indep_year")
    val population = column[Int]("population")
    val lifeExpectancy = column[Float]("life_expectancy")
    val gnp = column[BigDecimal]("gnp")
    val gnpOld = column[BigDecimal]("gnp_old")
    val localName = column[String]("local_name")
    val governmentForm = column[String]("government_form")
    val headOfState = column[String]("head_of_state")
    val capitalId = column[Long]("capital_id")
    val code2 = column[String]("code2")
  }

  Model.register[Country]

  class Language extends Model("language") {
    val id = column[Long]("id")
    val code = column[String]("code")
    val name = column[String]("name")
    val isOfficial = column[Boolean]("is_official")
    val percentage = column[BigDecimal]("percentage")
  }

  Model.register[Language]

  class CoinPrice extends Model("coin_price") {
    val uid = column[Long]("uid")
    val coin = column[String]("coin")
    val price = column[BigDecimal]("price")
    val created = column[Timestamp]("created")
  }

  Model.register[CoinPrice]

  class TempCoinPrice extends Model("temp_coin_price") {
    val uid = column[UUID]("uid")
    val coin = column[String]("coin")
    val price = column[BigDecimal]("price")
    val created = column[Timestamp]("created")
  }

  Model.register[TempCoinPrice]

  class BtcPrice extends Model("btc_price") {
    val uid = column[Long]("uid")
    val symbol = column[String]("symbol")
    val open = column[BigDecimal]("open")
    val close = column[BigDecimal]("close")
    val high = column[BigDecimal]("high")
    val low = column[BigDecimal]("low")
    val volBtc = column[BigDecimal]("vol_btc")
    val volUsd = column[BigDecimal]("vol_usd")
    val created = column[Timestamp]("created")
  }

  Model.register[BtcPrice]

  class Trip extends Model("trip") {
    val id = column[Long]("id")
    val cityId = column[Int]("city_id")
    val price = column[Int]("price")
  }

  Model.register[Trip]

  class Place extends Model("place") {
    val code = column[String]("code")
    val places = column[Seq[Jsonb]]("places")
  }

  Model.register[Place]
}









