package consumer.service

import fs2.kafka.ConsumerRecord
import consumer.config.consumerConfig.*
import cats.effect.kernel.*
import org.typelevel.log4cats.LoggerFactory
import fs2.kafka.ConsumerSettings
import fs2.kafka.*
import cats.effect.*
import cats.syntax.all.*
import cats.effect.kernel.Async
import fs2.Stream
import fs2.*

trait KafkaConsumerServiceDSL[F[_], A, B] {
  def consumeAndProcess(
      topic: String,
      process: ConsumerRecord[A, A] => F[Either[String, (A, B)]]
  ): F[Stream[F, Either[String, (A, B)]]]
}

class KafkaConsumerServiceLive[F[_]: Async: Concurrent, A, B] private (
    consumerSettings: ConsumerSettings[F, A, A],
    loggerFactory: LoggerFactory[F]
) extends KafkaConsumerServiceDSL[F, A, B] {

  private val logger = loggerFactory.getLogger

  override def consumeAndProcess(
      topic: String,
      process: ConsumerRecord[A, A] => F[Either[String, (A, B)]]
  ): F[Stream[F, Either[String, (A, B)]]] = {

    for {
      _ <- logger.info(s"Consuming from topic: $topic")
      res <- Async[F].pure(
        KafkaConsumer
          .stream(consumerSettings)
          .subscribeTo(topic)
          .partitionedRecords
          .flatMap { partitionStream =>
            partitionStream.map { committable =>
              Stream.eval(process(committable.record))
            }
          }
          .parJoinUnbounded
      )
    } yield res
  }

}
object KafkaConsumerServiceLive {

  def make[F[_]: Async, A, B](
      config: KafkaConsumerConfig,
      loggerFactory: LoggerFactory[F]
  )(implicit deserializer: Deserializer[F, A]): F[KafkaConsumerServiceLive[F, A, B]] = {

    val consumerSettings = ConsumerSettings(
      keyDeserializer = Deserializer[F, A],
      valueDeserializer = Deserializer[F, A]
    ).withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.bootstrapServers)
      .withGroupId(config.groupId)

    new KafkaConsumerServiceLive[F, A, B](consumerSettings, loggerFactory).pure[F]
  }
}
