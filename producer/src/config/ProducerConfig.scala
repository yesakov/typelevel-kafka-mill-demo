package producer.config

import pureconfig._
import pureconfig.generic.derivation.default._

object producerConfig {
  case class ProducerConfig(
      fetcherConfig: FetcherConfig,
      kafkaProducerConfig: KafkaProducerConfig
  ) derives ConfigReader

  case class FetcherConfig(
      apiEndpoint: String,
      cryptoIds: List[String],
      currencies: List[String],
      fetchDurationSec: Int
  ) derives ConfigReader

  case class KafkaProducerConfig(
      bootstrapServers: String,
      topic: String
  ) derives ConfigReader

}
