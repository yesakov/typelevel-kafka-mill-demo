package producer.test

import utest._
import producer.test.test_service.TestService._
import io.circe._
import io.circe.parser._
import shared.domain.cryptoPrice.*
import io.circe.syntax.*

object ProducerTest extends TestSuite {
  val tests = Tests {
    test("test CryptoPrice decode|encode") {
      val decodeResult = decode[CryptoPrices](testCryptoPricesJson)
      assert(decodeResult.isRight)

      val encoded = decode[CryptoPrices](decodeResult.toOption.get.asJson.noSpaces)
      assert(encoded.isRight)
      assert(encoded.toOption.get == decodeResult.toOption.get)
    }

    test("test ClientService fetchCryptoData") {
      val fetchResult = testClient.fetchCryptoData
      assert(fetchResult.isDefined)
      assert(fetchResult.get.contains("ethereum"))
    }
  }
}
