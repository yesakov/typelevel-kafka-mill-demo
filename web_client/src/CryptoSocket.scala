package web_client

import tyrian.websocket.*
import cats.effect.IO
import web_client.Msg

object CryptoSocket {

  def init(url: String) = {
    println("init websocket: " + url)
    WebSocket.connect[IO, Msg](
      address = url,
      onOpenMessage = "crypto ws connected",
      keepAliveSettings = KeepAliveSettings.default
    ) {
      case WebSocketConnect.Error(err) =>
        Msg.Error(err)

      case WebSocketConnect.Socket(ws) =>
        Msg.WsConnected(ws)
    }
  }
}
