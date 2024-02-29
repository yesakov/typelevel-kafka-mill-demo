package consumer.service

import fs2.kafka.ConsumerRecord
import io.circe._, io.circe.parser._
import consumer.config.consumerConfig.*
import cats.effect.kernel.Async
import cats.effect.*
import cats.syntax.all.*

object recordsService {

  class RecordsServiceLive[F[_]: Async, A, B: Decoder] private (
      recordsServiceConfig: RecordsServiceConfig,
      storeRecordToDb: (A, B) => F[Int],
      getLatestFromDb: F[List[(A, B)]]
  ) {

    private val recordsContainer = new RecordsContainer[A, B](recordsServiceConfig.maxRecordsSize)

    def deserializeRecord(record: ConsumerRecord[A, A]): Either[String, (A, B)] = {
      decode[B](record.value.toString())
        .map(res => (record.key, res))
        .left
        .map(err => err.toString)
    }

    def addRecord(record: (A, B)): F[Unit] = {
      for {
        _ <- Async[F].pure(recordsContainer.update(record._1, record._2))
        _ <- storeRecordToDb(record._1, record._2)
      } yield ()
    }

    def processKafkaRecord(record: ConsumerRecord[A, A]): F[Either[String, (A, B)]] = {
      for {
        recordE <- Async[F].pure(deserializeRecord(record))
        _       <- recordE.fold(err => Async[F].pure(()), record => addRecord(record._1, record._2))
      } yield recordE

    }

    def getLatestRecords: F[List[(A, B)]] = {
      val latest = recordsContainer.getLatest

      if (latest.isEmpty) {
        getLatestFromDb
      } else {
        Async[F].pure(latest)
      }
    }

    def getAllRecords: Map[A, List[B]] = {
      recordsContainer.getAllAsMap
    }
  }

  object RecordsServiceLive {
    def make[F[_]: Async, A, B: Decoder](
        recordsServiceConfig: RecordsServiceConfig,
        storeRecordToDb: (A, B) => F[Int],
        getLatestFromDb: F[List[(A, B)]]
    ): F[RecordsServiceLive[F, A, B]] =
      new RecordsServiceLive[F, A, B](recordsServiceConfig, storeRecordToDb, getLatestFromDb)
        .pure[F]
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
