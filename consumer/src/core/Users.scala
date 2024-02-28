package consumer.core

import java.util.UUID
import shared.domain.user.*

import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

trait Users[F[_]] { // "algebra"
  def create(user: User): F[UUID]
  def getAll: F[List[User]]
}

class UsersLive[F[_]: Concurrent] private (transactor: Transactor[F]) extends Users[F] {
  override def getAll: F[List[User]] =
    sql"""
      SELECT 
        username,
        email,
        age,
        gender,
        country,
        phone_number
      FROM users
    """
      .query[User]
      .stream
      .transact(transactor)
      .compile
      .toList

  override def create(user: User): F[UUID] =
    println("test test test " + user)
    sql"""
      INSERT INTO users(
        username,
        email,
        age,
        gender,
        country,
        phone_number
      ) VALUES (
        ${user.username},
        ${user.email},
        ${user.age},
        ${user.gender},
        ${user.country},
        ${user.phoneNumber}
      )
    """.update
      .withUniqueGeneratedKeys[UUID]("user_id")
      .transact(transactor)
}

object UsersLive {
  def make[F[_]: Concurrent](postgres: Transactor[F]): F[UsersLive[F]] =
    new UsersLive[F](postgres).pure[F]

  def resource[F[_]: Concurrent](postgres: Transactor[F]): Resource[F, UsersLive[F]] =
    Resource.pure(new UsersLive[F](postgres))
}
