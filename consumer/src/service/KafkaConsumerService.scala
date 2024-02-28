package consumer.service

import fs2.kafka.ConsumerRecord
import consumer.config.consumerConfig.*
import cats.effect.kernel.*
import org.typelevel.log4cats.LoggerFactory
import fs2.kafka.ConsumerSettings
import fs2.kafka.*
import cats.effect.*
import cats.syntax.all._
import cats.effect.kernel.Async
import fs2.Stream
import fs2.*

trait KafkaConsumerServiceDSL[F[_], A, B] {
  def consumeAndProcess(
      topic: String,
      process: ConsumerRecord[A, A] => Either[String, (A, B)]
  ): F[Stream[F, Either[String, (A, B)]]]
}

class KafkaConsumerServiceLive[F[_]: Async: Concurrent, A, B] private (
    kafkaConsumerConfig: KafkaConsumerConfig,
    loggerFactory: LoggerFactory[F]
)(implicit deserializer: Deserializer[F, A])
    extends KafkaConsumerServiceDSL[F, A, B] {

  private val logger = loggerFactory.getLogger

  private val consumerSettings = ConsumerSettings(
    keyDeserializer = Deserializer[F, A],
    valueDeserializer = Deserializer[F, A]
  ).withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withBootstrapServers(kafkaConsumerConfig.bootstrapServers)
    .withGroupId(kafkaConsumerConfig.groupId)

  private val kafkaConsumer = KafkaConsumer.stream(consumerSettings)

  override def consumeAndProcess(
      topic: String,
      process: ConsumerRecord[A, A] => Either[String, (A, B)]
  ): F[Stream[F, Either[String, (A, B)]]] = {

    val a123: F[Stream[F, Either[String, (A, B)]]] = Async[F]
      .pure(
        kafkaConsumer
          .subscribeTo(topic)
          .partitionedRecords
          .map { partitionStream =>
            partitionStream.map { committable =>
              process(committable.record)
            // committable.record
            }
          }
          .parJoinUnbounded
      )

    //   for {
    //   _ <- logger.info(s"Recieving data from Kafka topic: '$topic'")
    //   s <- kafkaConsumer
    //     .subscribeTo(topic)
    //     .partitionedRecords
    //     .map { partitionStream =>
    //       partitionStream.map { committable =>
    //         process(committable.record)
    //       // committable.record
    //       }
    //     }
    //     .parJoinUnbounded
    //   // .parJoinUnbounded
    // } yield s

    a123
  }
}

object KafkaConsumerServiceLive {
  def resource[F[_]: Async, A, B](
      config: KafkaConsumerConfig,
      loggerFactory: LoggerFactory[F]
  )(implicit deserializer: Deserializer[F, A]): Resource[F, KafkaConsumerServiceLive[F, A, B]] =
    Resource.pure(new KafkaConsumerServiceLive[F, A, B](config, loggerFactory))
}
