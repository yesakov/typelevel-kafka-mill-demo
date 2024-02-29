package consumer.service

import shared.domain.cryptoPrice.*
import cats.effect.kernel.Async
import doobie.implicits.*
import doobie.util.transactor.Transactor
import cats.syntax.all.*
import org.typelevel.log4cats.LoggerFactory
import doobie.util.log.LogHandler
import doobie.util.log.LogEvent
import org.typelevel.log4cats.Logger

trait PostgresServiceDsl[F[_]] {
  def addCryptoPriceRecord(crypto: String, price: CryptoPrice): F[Int]
  def getLatestPrices: F[List[(String, CryptoPrice)]]
  def removeCryptoPriceRecord(id: Int): F[Int]
}

class PostgresServiceLive[F[_]: Async] private (
    transactor: Transactor[F],
    logger: Logger[F]
) extends PostgresServiceDsl[F] {

  override def getLatestPrices: F[List[(String, CryptoPrice)]] = {
    for {
      _ <- logger.info("get latest prices")
      result <- sql"""WITH ranked_prices AS (
        SELECT
            c.name,
            p.usd,
            p.usd_24h_change,
            p.eur,
            p.eur_24h_change,
            p.uah,
            p.uah_24h_change,
            EXTRACT(EPOCH FROM p.last_updated_at) AS last_updated_at_millis,
            ROW_NUMBER() OVER (PARTITION BY p.crypto_id ORDER BY p.last_updated_at DESC) AS rn
        FROM crypto_prices p
        JOIN cryptocurrencies c ON p.crypto_id = c.id
        )
        SELECT name, usd, usd_24h_change, eur, eur_24h_change, uah, uah_24h_change, last_updated_at_millis
        FROM ranked_prices
        WHERE rn = 1;"""
        .query[(String, CryptoPrice)]
        .to[List]
        .transact(transactor)
    } yield result
  }

  override def addCryptoPriceRecord(crypto: String, price: CryptoPrice): F[Int] =
    for {
      _ <- logger.info("insert crypto price record")
      result <-
        sql"""INSERT INTO crypto_prices (crypto_id, usd, usd_24h_change, eur, eur_24h_change, uah, uah_24h_change, last_updated_at) 
            VALUES (
            (SELECT id FROM cryptocurrencies WHERE name = ${crypto}), 
            ${price.usd}, ${price.usd_24hChange}, ${price.eur}, ${price.eur_24hChange}, 
            ${price.uah}, ${price.uah_24hChange}, to_timestamp(${price.lastUpdatedAt}) AT TIME ZONE 'UTC'
        );""".update
          .withUniqueGeneratedKeys[Int]("id")
          .transact(transactor)
    } yield result

  override def removeCryptoPriceRecord(id: Int): F[Int] = {
    for {
      _      <- logger.info("delete crypto price record")
      result <- sql"""DELETE FROM crypto_prices WHERE id = ${id};""".update.run.transact(transactor)
    } yield result
  }
}

object PostgresServiceLive {
  def make[F[_]: Async](
      loggerFactory: LoggerFactory[F]
  ): F[PostgresServiceLive[F]] = {

    val logger: Logger[F] = loggerFactory.getLogger

    val transactor = Transactor
      .fromDriverManager[F]
      .apply(
        driver = "org.postgresql.Driver",          // JDBC driver classname
        url = "jdbc:postgresql://localhost:5444/", // Connect URL - Driver specific
        user = "docker",                           // Database user name
        password = "docker",                       // Database password
        logHandler = Some(new LogHandler[F] { // Log Handler
          def run(logEvent: LogEvent): F[Unit] = logger.info(logEvent.sql)
        })
      )

    new PostgresServiceLive[F](transactor, logger).pure[F]
  }
}
