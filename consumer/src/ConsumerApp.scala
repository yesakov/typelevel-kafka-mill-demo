package consumer

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import pureconfig.ConfigSource
import consumer.config.consumerConfig.*
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.LoggerFactory
import consumer.service.KafkaConsumerServiceLive
import consumer.service.recordsService.RecordsServiceLive
import shared.domain.cryptoPrice.*
import consumer.http.CryptoRouter

object Application extends IOApp.Simple {

  val config                                = ConfigSource.default.loadOrThrow[ConsumerConfig]
  def logging[F[_]: Sync]: LoggerFactory[F] = Slf4jFactory.create[F]

  override def run: IO[Unit] = {
    for {
      kafkaPConsumer <- KafkaConsumerServiceLive
        .make[IO, String, CryptoPrice](config.kafkaConsumerConfig, logging[IO])

      recordsServiceLive <- RecordsServiceLive
        .make[IO, String, CryptoPrice](config.recordsServiceConfig)

      cryptoStream <- kafkaPConsumer
        .consumeAndProcess(
          config.kafkaConsumerConfig.topic,
          recordsServiceLive.processKafkaRecord
        )

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Hostname.fromString(config.serverConfig.host).getOrElse(host"localhost"))
        .withPort(Port.fromString(config.serverConfig.port).getOrElse(port"4041"))
        .withHttpWebSocketApp(wsb =>
          CORS(
            CryptoRouter[IO, String, CryptoPrice](
              recordsServiceLive,
              wsb,
              cryptoStream,
              logging[IO]
            ).routes.orNotFound
          )
        )
        .build
        .use(_ => logging[IO].getLogger.info("consumer server ready") *> IO.never)
    } yield ()
  }
}
