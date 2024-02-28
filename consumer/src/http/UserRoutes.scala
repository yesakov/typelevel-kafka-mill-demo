package consumer.http

import cats.effect.*
import cats.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import io.circe.generic.auto.*

import org.http4s.server.Router
import consumer.core.Users
import shared.domain.user.User

class UserRoutes[F[_]: Concurrent] private (users: Users[F]) extends Http4sDsl[F] {
  private val prefix = "/users"

  // post /users/create { User }
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        user <- req.as[User]
        id   <- users.create(user)
        resp <- Created(id)
      } yield resp
  }

  // get /users
  private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    users.getAll.flatMap(users => Ok(users))
  }

  val routes: HttpRoutes[F] = Router(
    prefix -> (createUserRoute <+> getAllRoute)
  )
}

object UserRoutes {
  def resource[F[_]: Concurrent](users: Users[F]): Resource[F, UserRoutes[F]] =
    Resource.pure(new UserRoutes[F](users))
}
