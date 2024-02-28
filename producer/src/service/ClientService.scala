package producer.service

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.GET
import org.http4s._
import org.http4s.client.Client
import cats.effect.*
import cats.implicits.*
import producer.config.producerConfig.*
import shared.domain.cryptoPrice.*
import io.circe._, io.circe.parser._
import org.typelevel.log4cats.LoggerFactory

trait ClientDSL[F[_]] {
  val clientR: Resource[F, Client[F]]
  def fetchCryptoData: F[CryptoPrices]
}

class ClientLive[F[_]: Async] private (
    config: FetcherConfig,
    loggerFactory: LoggerFactory[F]
) extends ClientDSL[F] {
  val logger           = loggerFactory.getLogger
  override val clientR = EmberClientBuilder.default[F].build

  private val requestUri = Uri
    .unsafeFromString(config.apiEndpoint)
    .withQueryParam("ids", config.cryptoIds.mkString(","))
    .withQueryParam("vs_currencies", config.currencies.mkString(","))
    .withQueryParam("include_24hr_change", "true")
    .withQueryParam("include_last_updated_at", "true")

  private val request: Request[F] = Request(uri = requestUri, method = GET)

  override def fetchCryptoData: F[CryptoPrices] = {
    clientR.use { client =>
      for {
        _        <- logger.info("fetching crypto data")
        response <- client.expect[String](request).onError(e => logger.error(e.toString))
        parsed <- Async[F].delay(
          decode[CryptoPrices](response).fold(
            err => {
              logger.error(err.toString)
              Map.empty
            },
            res => res
          )
        )
        _ <- logger.info(s"get data for ids: ${parsed.keySet.mkString(", ")}")
      } yield (parsed)
    }
  }
}

object ClientLive {
  def resource[F[_]: Async](
      config: FetcherConfig,
      loggerFactory: LoggerFactory[F]
  ): Resource[F, ClientLive[F]] =
    Resource.pure(new ClientLive[F](config, loggerFactory))
}
