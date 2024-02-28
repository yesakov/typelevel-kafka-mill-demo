package producer

import cats.effect.{ExitCode, IO, IOApp}
import scala.concurrent.duration._
import cats.effect.*
import cats.Applicative
import producer.config.producerConfig.*
import pureconfig._
import producer.service.ClientLive
import shared.domain.cryptoPrice.*
import cats.syntax.all._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import producer.service.KafkaProducerLive
import producer.service.KafkaProducerDSL
import shared.domain.cryptoPrice
import io.circe.syntax.*

object Producer extends IOApp {

  val config                                = ConfigSource.default.loadOrThrow[ProducerConfig]
  def logging[F[_]: Sync]: LoggerFactory[F] = Slf4jFactory.create[F]

  def schedule[F[_]: Applicative: Async: Temporal, A](scheduled: F[A]): F[A] =
    for {
      _   <- Temporal[F].sleep(config.fetcherConfig.fetchDurationSec.seconds)
      res <- scheduled
    } yield res

  def repeat[F[_]: Async: Temporal, A](toRepeat: F[A], process: A => F[Unit]): F[Unit] =
    for {
      res <- schedule[F, A](toRepeat)
      _   <- process(res)
      _   <- repeat(toRepeat, process)
    } yield ()

    //  fs2.Stream.awakeDelay[IO](duration).as(1).scan[Int](0)(_ + _),

  def processCryptoData(kafkaProducer: KafkaProducerDSL[IO, String])(
      cryptoPrices: CryptoPrices
  ): IO[Unit] = {
    val priceList: List[(String, String)] =
      cryptoPrices.toList.map(cp => (cp._1, cp._2.asJson.noSpaces))

    kafkaProducer.sendToKafka(config.kafkaProducerConfig.topic, priceList)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      client <- ClientLive.resource[IO](config.fetcherConfig, logging[IO]).use(IO.pure)
      kafkaProducer <- KafkaProducerLive
        .resource[IO, String](config.kafkaProducerConfig, logging[IO])
        .use(IO.pure)
      _ <- repeat[IO, CryptoPrices](client.fetchCryptoData, processCryptoData(kafkaProducer))
    } yield ExitCode.Success
  }
}
