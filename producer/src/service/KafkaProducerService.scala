package producer.service

import producer.config.producerConfig.KafkaProducerConfig
import org.typelevel.log4cats.LoggerFactory
import cats.effect.*
import cats.syntax.all._
import cats.effect.kernel.Async
import fs2.kafka.*
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings}
import fs2.Chunk

trait KafkaProducerDSL[F[_], A] {
  def sendToKafka(topic: String, data: List[(A, A)]): F[Unit]
}

class KafkaProducerLive[F[_]: Async: Concurrent, A] private (
    kafkaProducerConfig: KafkaProducerConfig,
    loggerFactory: LoggerFactory[F]
)(implicit serializer: Serializer[F, A])
    extends KafkaProducerDSL[F, A] {

  private val logger = loggerFactory.getLogger

  private val producerSettings: ProducerSettings[F, A, A] =
    ProducerSettings(
      keySerializer = Serializer[F, A],
      valueSerializer = Serializer[F, A]
    ).withBootstrapServers(kafkaProducerConfig.bootstrapServers)

  private val kafkaProducer = KafkaProducer.stream(producerSettings)

  private def createRecords(data: List[(A, A)], topic: String): List[ProducerRecord[A, A]] = {
    data.map { case (key, value) => ProducerRecord(topic, key, value) }
  }

  override def sendToKafka(topic: String, data: List[(A, A)]): F[Unit] = {

    for {
      _ <- logger.info(s"Sending data to Kafka topic: '$topic'")
      count <- kafkaProducer
        .evalMap { producer =>
          producer
            .produce(Chunk.seq(createRecords(data, topic)))
            .flatten
            .onError(err => logger.error(err.toString))
        }
        .compile
        .count
      _ <- logger.info(s"Sent count: $count")
    } yield ()
  }
}

object KafkaProducerLive {
  def resource[F[_]: Async, A](
      config: KafkaProducerConfig,
      loggerFactory: LoggerFactory[F]
  )(implicit serializer: Serializer[F, A]): Resource[F, KafkaProducerLive[F, A]] =
    Resource.pure(new KafkaProducerLive[F, A](config, loggerFactory))
}
