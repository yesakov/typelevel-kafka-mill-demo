package consumer.service

import fs2.kafka.ConsumerRecord
import io.circe._, io.circe.parser._
import consumer.config.consumerConfig.*
import cats.effect.kernel.Async
import cats.effect.*
import cats.syntax.all.*

object recordsService {

  class RecordsServiceLive[A, B: Decoder] private (
      recordsServiceConfig: RecordsServiceConfig
  ) {

    private val recordsContainer = new RecordsContainer[A, B](recordsServiceConfig.maxRecordsSize)

    def deserializeRecord(record: ConsumerRecord[A, A]): Either[String, (A, B)] = {
      decode[B](record.value.toString())
        .map(res => (record.key, res))
        .left
        .map(err => err.toString)
    }

    def addRecord(record: (A, B)): Unit = {
      recordsContainer.update(record._1, record._2)
    }

    def processKafkaRecord(record: ConsumerRecord[A, A]): Either[String, (A, B)] = {
      deserializeRecord(record).map { record =>
        addRecord(record._1, record._2)
        record
      }
    }

    def getLatestRecords: List[(A, B)] = {
      recordsContainer.getLatest
    }

    def getAllRecords: Map[A, List[B]] = {
      recordsContainer.getAllAsMap
    }
  }

  object RecordsServiceLive {
    def make[F[_]: Async, A, B: Decoder](
        recordsServiceConfig: RecordsServiceConfig
    ): F[RecordsServiceLive[A, B]] =
      new RecordsServiceLive[A, B](recordsServiceConfig).pure[F]
  }

  class RecordsList[A](maxSize: Int) {
    private val list: scala.collection.mutable.ListBuffer[A] = scala.collection.mutable.ListBuffer()
    def add(elem: A): RecordsList[A] = {
      if (list.size < maxSize) {
        list.prepend(elem)
      } else {
        list.remove(maxSize - 1)
        list.prepend(elem)
      }
      this
    }

    def head: A = list.head

    def getAll: List[A] = list.toList
  }

  class RecordsContainer[A, B](maxSize: Int) {
    private val records: scala.collection.mutable.Map[A, RecordsList[B]] =
      scala.collection.mutable.Map.empty

    def update(k: A, v: B): RecordsContainer[A, B] = {
      val list = records.get(k).fold(new RecordsList[B](maxSize))(value => value.add(v))
      records.update(k, list)
      this
    }

    def get(k: A): Option[RecordsList[B]] = {
      records.get(k)
    }

    def getLatest: List[(A, B)] = {
      records.map({ case (k, v) => (k, v.head) }).toList
    }

    def getAllAsMap: Map[A, List[B]] = records.toMap.map({ case (k, v) => (k, v.getAll) })
  }
}
