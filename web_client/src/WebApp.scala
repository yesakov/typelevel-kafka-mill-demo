package web_client

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import cats.effect.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import shared.domain.cryptoPrice.CryptoPrice
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.websocket.*
import io.circe.parser.decode as jsonDecode

enum Msg {
  case NoMsg
  case LoadCryptos(cryptos: List[(String, CryptoPrice)])
  case UpdateCryptos(crypto: (String, CryptoPrice))
  case Error(e: String)
  case WsConnected(ws: WebSocket[IO])
}

case class Model(
    cryptoSocket: Option[WebSocket[IO]] = None,
    cryptos: List[(String, CryptoPrice, Option[Boolean])] = List(),
    error: Option[String] = None
)

@JSExportTopLevel("CryptoClientApp")
object Main extends TyrianIOApp[Msg, Model] {

  def router: Location => Msg = Routing.none(Msg.NoMsg)

  def backendCall: Cmd[IO, Msg] =
    Http.send(
      Request.get("http://localhost:4041/crypto"),
      Decoder[Msg](
        resp =>
          parse(resp.body).flatMap(_.as[List[(String, CryptoPrice)]]) match {
            case Left(e) =>
              Msg.Error("get cryptos error: " + e.getMessage())
            case Right(list) => Msg.LoadCryptos(list)
          },
        err => Msg.Error("request error: " + err.toString())
      )
    )

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    (Model(), backendCall combine CryptoSocket.init("ws://localhost:4041/crypto/ws"))
  }

  override def view(model: Model): Html[Msg] =
    div(`class` := "row")(
      if (model.error.isDefined) {
        div(`class` := "col")(
          div(`class` := "alert alert-danger")(
            model.error.get
          )
        )
      } else {
        table(
          tableHeader ::
            model.cryptos.sortBy(x => x._2.usd)(Ordering[BigDecimal].reverse).map { crypto =>
              cryptoToRow(crypto)
            }
        )
      }
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = msg =>
    msg match {
      case Msg.NoMsg    => (model, Cmd.None)
      case Msg.Error(e) => (model.copy(error = Some(e)), Cmd.None)
      case Msg.LoadCryptos(list) =>
        (model.copy(cryptos = list.map(x => (x._1, x._2, None))), Cmd.None)
      case Msg.UpdateCryptos(list) => (updateModelWs(model, list), Cmd.None)
      case Msg.WsConnected(ws) =>
        (model.copy(cryptoSocket = Some(ws)), Cmd.None)
    }

  override def subscriptions(model: Model): Sub[IO, Msg] = {
    model.cryptoSocket match {
      case Some(ws) =>
        ws.subscribe {
          case WebSocketEvent.Error(errorMesage) =>
            Msg.Error(errorMesage)

          case WebSocketEvent.Receive(message) =>
            println("receive: " + message)
            jsonDecode[(String, CryptoPrice)](message) match {
              case Right(crypto) => Msg.UpdateCryptos(crypto)
              case Left(err)     => Msg.Error(err.getMessage())
            }

          case WebSocketEvent.Open =>
            Msg.NoMsg

          case WebSocketEvent.Close(code, reason) =>
            Msg.NoMsg

          case WebSocketEvent.Heartbeat =>
            Msg.NoMsg
        }
      case None =>
        Sub.emit(Msg.NoMsg)
    }

  }

  def updateModelWs(model: Model, crypto: (String, CryptoPrice)): Model = {
    val cryptos       = model.cryptos
    val currentCrypto = cryptos.find(x => x._1 == crypto._1)
    currentCrypto match
      case None => model.copy(cryptos = model.cryptos :+ (crypto._1, crypto._2, None))
      case Some(cc) => {
        val status =
          if (cc._2.usd_24hChange > crypto._2.usd_24hChange) Some(false)
          else if (cc._2.usd_24hChange < crypto._2.usd_24hChange) Some(true)
          else cc._3
        model.copy(cryptos =
          cryptos.filter(x => x._1 != crypto._1) :+ (crypto._1, crypto._2, status)
        )
      }
  }

  def cryptoToRow(crypto: (String, CryptoPrice, Option[Boolean])) = {
    def arrow = {
      crypto._3 match {
        case Some(true)  => span(`class` := "price-up")(" ↑")
        case Some(false) => span(`class` := "price-down")(" ↓")
        case None        => span(`class` := "arrow")("")
      }
    }

    tr(
      td(
        span(`class` := "crypto-name")(crypto._1),
        arrow
      ),
      td(crypto._2.usd.toString()),
      td(crypto._2.usd_24hChange.toString()),
      td(crypto._2.eur.toString()),
      td(crypto._2.eur_24hChange.toString()),
      td(crypto._2.uah.toString()),
      td(crypto._2.uah_24hChange.toString())
    )
  }

  val tableHeader = tr(
    th("Crypto"),
    th("USD"),
    th("USD 24h Change"),
    th("EUR"),
    th("EUR 24h Change"),
    th("UAH"),
    th("UAH 24h Change")
  )
}
