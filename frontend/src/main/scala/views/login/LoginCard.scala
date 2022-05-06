package views.login

import com.raquo.laminar.api.L._
import components.card.Card

case class LoginCard(loginForm: LoginForm)
    extends Card(
      mTitle = "Society Management Login",
      cardContent = Some(loginForm)
    )
