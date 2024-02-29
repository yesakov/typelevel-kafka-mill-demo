import utest._
import shared.domain.cryptoPrice.*
import consumer.service.PostgresServiceLive
import cats.effect.IO
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.effect.unsafe.implicits.global

object ConsumerTest extends TestSuite {
  val posgresLive = PostgresServiceLive.make[IO](Slf4jFactory.create[IO]).unsafeRunSync()

  val cryptoPrice1 = CryptoPrice(
    BigDecimal(54000),
    BigDecimal(2.00),
    BigDecimal(50000),
    BigDecimal(1.80),
    BigDecimal(1600000),
    BigDecimal(2.50),
    2409151252L
  )

  val cryptoPrice2 = CryptoPrice(
    BigDecimal(54001),
    BigDecimal(2.01),
    BigDecimal(50001),
    BigDecimal(1.81),
    BigDecimal(1600001),
    BigDecimal(2.51),
    2409152252L
  )

  val cryptoName = "bitcoin"

  val tests = Tests {
    test("insert/retrieve/delete crypto price to/from db") {
      val insertResult1 = posgresLive.addCryptoPriceRecord(cryptoName, cryptoPrice1).unsafeRunSync()
      val insertResult2 = posgresLive.addCryptoPriceRecord(cryptoName, cryptoPrice2).unsafeRunSync()
      assert(insertResult1 != insertResult2)

      val latestResult = posgresLive.getLatestPrices.unsafeRunSync()
      val res          = latestResult.toMap.get(cryptoName).get
      assert(cryptoPrice2 == res)

      val removeResult1 = posgresLive.removeCryptoPriceRecord(insertResult1).unsafeRunSync()
      posgresLive.removeCryptoPriceRecord(insertResult2).unsafeRunSync()

      assert(removeResult1 == 1)
    }
  }
}
