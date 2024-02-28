package shared.domain

import io.circe.generic.auto._
import io.circe.derivation.{Configuration, ConfiguredEncoder, ConfiguredDecoder}
import io.circe.{Encoder, Decoder}

object cryptoPrice {
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

  case class CryptoPrice(
      usd: BigDecimal,
      usdMarketCap: BigDecimal,
      usd_24h_vol: BigDecimal,
      usd_24hChange: BigDecimal,
      eur: BigDecimal,
      eurMarketCap: BigDecimal,
      eur_24h_vol: BigDecimal,
      eur_24hChange: BigDecimal,
      uah: BigDecimal,
      uahMarketCap: BigDecimal,
      uah_24h_vol: BigDecimal,
      uah_24hChange: BigDecimal,
      lastUpdatedAt: Long
  ) derives ConfiguredEncoder,
        ConfiguredDecoder

  type CryptoPrices = Map[String, CryptoPrice]
}
