package consumer

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import doobie._
import doobie.util.log.LogEvent
import cats.implicits._

import consumer.core.UsersLive
import consumer.http.UserRoutes
import doobie.util.transactor.Transactor
import pureconfig.ConfigSource

import consumer.config.consumerConfig.*
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.LoggerFactory
import consumer.service.KafkaConsumerServiceLive
import consumer.service.recordsService.RecordsServiceLive
import shared.domain.cryptoPrice.*
import consumer.http.CryptoRouter

object Application extends IOApp.Simple {

  // val postgres = Transactor
  //   .fromDriverManager[IO]
  //   .apply(
  //     driver = "org.postgresql.Driver",          // JDBC driver classname
  //     url = "jdbc:postgresql://localhost:5444/", // Connect URL - Driver specific
  //     user = "docker",                           // Database user name
  //     password = "docker",                       // Database password
  //     logHandler = Some(new LogHandler[IO] {
  //       def run(logEvent: LogEvent): IO[Unit] = IO { println(logEvent.sql) }
  //     }) // Here we specify our log event handler
  //   )

  val config                                = ConfigSource.default.loadOrThrow[ConsumerConfig]
  def logging[F[_]: Sync]: LoggerFactory[F] = Slf4jFactory.create[F]

  override def run: IO[Unit] = {
    for {
      kafkaPConsumer <- KafkaConsumerServiceLive
        .resource[IO, String, CryptoPrice](config.kafkaConsumerConfig, logging[IO])
        .use(IO.pure)

      recordsServiceLive <- RecordsServiceLive
        .resource[IO, String, CryptoPrice](config.recordsServiceConfig)
        .use(IO.pure)

      cryptoStream <- kafkaPConsumer
        .consumeAndProcess(
          config.kafkaConsumerConfig.topic,
          recordsServiceLive.processKafkaRecord
        )
      // .foreverM
      // .start

      // cryptoApi <- CryptoRouter
      //   .resource[IO, String, CryptoPrice](recordsServiceLive, logging[IO])
      //   .use(IO.pure)

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(Hostname.fromString(config.serverConfig.host).getOrElse(host"localhost"))
        .withPort(Port.fromString(config.serverConfig.port).getOrElse(port"4041"))
        .withHttpWebSocketApp(wsb =>
          CORS(
            CryptoRouter(recordsServiceLive, wsb, cryptoStream, logging[IO]).routes.orNotFound
          )
        )
        // .withHttpApp(CORS(cryptoApi.routes.orNotFound))
        .build
        .use(_ => logging[IO].getLogger.info("consumer server ready") *> IO.never)
    } yield ()
  }
}
