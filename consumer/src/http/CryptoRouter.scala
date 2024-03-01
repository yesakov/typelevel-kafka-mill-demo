package consumer.http

import cats.effect.*
import cats.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import io.circe.Encoder
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import fs2.{Stream, Pipe}
import scala.concurrent.duration.*
import io.circe.syntax.*
import fs2.concurrent.Topic
import consumer.service.PostgresServiceDsl

class CryptoRouter[F[_]: Async: Concurrent: Temporal, A: Encoder, B: Encoder] private (
    wsb: WebSocketBuilder2[F],
    topic: Topic[F, Either[String, (A, B)]],
    postgresService: PostgresServiceDsl[F],
    loggerFactory: LoggerFactory[F]
) extends Http4sDsl[F] {

  private val logger = loggerFactory.getLogger
  private val prefix = "/crypto"

  private val getLatestCryptoData: HttpRoutes[F] = HttpRoutes.of[F] {
    // get /crypto
    case GET -> Root =>
      for {
        _            <- logger.info("GET latest crypto data")
        latestCrypto <- postgresService.getLatestPrices
        resp         <- Ok(latestCrypto)
      } yield resp

    // ws /crypto/ws
    case GET -> Root / "ws" =>
      val toClientStream: Stream[F, WebSocketFrame] = topic
        .subscribe(1000)
        .filter(_.isRight)
        .map(_.toOption.get)
        .map(record => WebSocketFrame.Text(record.asJson.noSpaces))

      wsb.build(
        Stream(
          Stream
            .awakeEvery[F](30.seconds)
            .map(_ => WebSocketFrame.Ping()),
          toClientStream
        ).parJoinUnbounded,   // send
        handleRecieveStream() // recieve
      )

  }

  private def handleRecieveStream(): Pipe[F, WebSocketFrame, Unit] = { wsf =>
    wsf
      .evalMap {
        case WebSocketFrame.Text(t) => logger.info("recieved message: " + t)
        case WebSocketFrame.Ping(_) => logger.info("recieved ping")
        case _                      => logger.info("unknown message")
      }
  }

  val routes: HttpRoutes[F] = Router(prefix -> getLatestCryptoData)

}

object CryptoRouter {
  def apply[F[_]: Async: Concurrent: Temporal, A: Encoder, B: Encoder](
      wsb: WebSocketBuilder2[F],
      topic: Topic[F, Either[String, (A, B)]],
      postgresService: PostgresServiceDsl[F],
      loggerFactory: LoggerFactory[F]
  ): CryptoRouter[F, A, B] = {
    new CryptoRouter[F, A, B](wsb, topic, postgresService, loggerFactory)
  }

  def make[F[_]: Async: Concurrent: Temporal, A: Encoder, B: Encoder](
      wsb: WebSocketBuilder2[F],
      topic: Topic[F, Either[String, (A, B)]],
      postgresService: PostgresServiceDsl[F],
      loggerFactory: LoggerFactory[F]
  ): F[CryptoRouter[F, A, B]] = {
    new CryptoRouter[F, A, B](wsb, topic, postgresService, loggerFactory).pure[F]
  }
}
