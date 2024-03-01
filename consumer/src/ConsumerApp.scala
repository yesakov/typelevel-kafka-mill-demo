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
import shared.domain.cryptoPrice.*
import consumer.http.CryptoRouter
import consumer.service.PostgresServiceLive
import fs2.Stream
import fs2.concurrent.Topic

object Application extends IOApp.Simple {

  val config                                = ConfigSource.default.loadOrThrow[ConsumerConfig]
  def logging[F[_]: Sync]: LoggerFactory[F] = Slf4jFactory.create[F]

  override def run: IO[Unit] = {
    for {

      postgresService <- PostgresServiceLive.make[IO](logging[IO])

      kafkaConsumer <- KafkaConsumerServiceLive
        .make[IO, String, CryptoPrice](config.kafkaConsumerConfig, logging[IO])

      // recordsService <- RecordsServiceLive
      //   .make[IO, String, CryptoPrice](
      //     config.recordsServiceConfig,
      //     postgresService.addCryptoPriceRecord,
      //     postgresService.getLatestPrices
      //   )

      cryptoStream <- kafkaConsumer
        .consumeAndProcess(
          config.kafkaConsumerConfig.topic,
          postgresService.addCryptoPriceRecord
        )
        .recover(_ => Stream.emit(Left("Kafka Stream Error")))

      topic <- Topic[IO, Either[String, (String, CryptoPrice)]]

      _ <- cryptoStream.through(topic.publish).compile.drain.start

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Hostname.fromString(config.serverConfig.host).getOrElse(host"localhost"))
        .withPort(Port.fromString(config.serverConfig.port).getOrElse(port"4041"))
        .withHttpWebSocketApp(wsb =>
          CORS(
            CryptoRouter[IO, String, CryptoPrice](
              wsb,
              topic,
              postgresService,
              logging[IO]
            ).routes.orNotFound
          )
        )
        .build
        .use(_ => logging[IO].getLogger.info("consumer server ready") *> IO.never)
    } yield ()
  }
}
