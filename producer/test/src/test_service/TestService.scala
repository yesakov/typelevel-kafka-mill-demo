package producer.test.test_service

import producer.service.ClientDSL
import org.http4s.client.Client
import cats.effect.kernel.Resource
import shared.domain.cryptoPrice.*
import io.circe._, io.circe.parser._

object TestService {
  val testClient = new ClientDSL[Option] {

    override val clientR: Resource[Option, Client[Option]] = {
      Resource.apply[Option, Client[Option]](Option.empty)
    }

    override def fetchCryptoData: Option[CryptoPrices] = {
      decode[CryptoPrices](testCryptoPricesJson).toOption
    }
  }

  val testCryptoPricesJson = """
                                |{
                                |  "bitcoin": {
                                |    "usd": 50000,
                                |    "usd_market_cap": 1000000000,
                                |    "usd_24h_vol": 10000000,
                                |    "usd_24h_change": 5,
                                |    "eur": 45000,
                                |    "eur_market_cap": 900000000,
                                |    "eur_24h_vol": 9000000,
                                |    "eur_24h_change": 4.5,
                                |    "uah": 1500000,
                                |    "uah_market_cap": 30000000000,
                                |    "uah_24h_vol": 30000000,
                                |    "uah_24h_change": 6,
                                |    "last_updated_at": 1625240447
                                |  },
                                |  "ethereum": {
                                |    "usd": 2440.67,
                                |    "usd_market_cap": 293499070279.55664,
                                |    "usd_24h_vol": 12219886827.375391,
                                |    "usd_24h_change": 2.8252451148570112,
                                |    "eur": 2267.45,
                                |    "eur_market_cap": 272716401113.06134,
                                |    "eur_24h_vol": 11352629239.349724,
                                |    "eur_24h_change": 2.7863003911316184,
                                |    "uah": 91678,
                                |    "uah_market_cap": 11024649518588.537,
                                |    "uah_24h_vol": 459013274898.3182,
                                |    "uah_24h_change": 2.718091030692413,
                                |    "last_updated_at": 1707409638
                                |   }
                                |}
                                |""".stripMargin
}
