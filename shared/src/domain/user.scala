package shared.domain

object user {
  case class User(
      username: String,
      email: String,
      age: Option[Int],
      gender: Option[String],
      country: Option[String],
      phoneNumber: Option[String]
  )

  object User {
    val dummyUser: User = User(
      username = "JohnDoe",
      email = "john.doe@example.com",
      age = Some(28),
      gender = Some("Male"),
      country = Some("USA"),
      phoneNumber = Some("+1 (555) 123-4567")
    )
  }
}
