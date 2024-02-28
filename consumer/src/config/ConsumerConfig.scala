package consumer.config

import pureconfig._
import pureconfig.generic.derivation.default._

object consumerConfig {

  case class ConsumerConfig(
      kafkaConsumerConfig: KafkaConsumerConfig,
      recordsServiceConfig: RecordsServiceConfig,
      serverConfig: ServerConfig
  ) derives ConfigReader

  case class KafkaConsumerConfig(
      bootstrapServers: String,
      groupId: String,
      topic: String
  ) derives ConfigReader

  case class RecordsServiceConfig(
      maxRecordsSize: Int
  ) derives ConfigReader

  case class ServerConfig(
      host: String,
      port: String
  ) derives ConfigReader
}
