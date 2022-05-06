package shared.pages

sealed trait Page {
  type PageType
  type Args
  val args: Args
}

sealed case class RootPage() extends Page {
  type PageType = RootPage
  type Args     = Unit
  val args: Unit = ()

}

sealed case class FormResourcesPage() extends Page {
  type PageType = FormResourcesPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class FormResourcePage(id: String) extends Page {
  type PageType = FormResourcePage
  type Args     = String
  val args: String = id

}

sealed case class LoginPage() extends Page {
  type PageType = LoginPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class UserAccountPage() extends Page {
  type PageType = UserAccountPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class UserRegistrationPage() extends Page {
  type PageType = UserRegistrationPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class SocietyRegistrationPage() extends Page {
  type PageType = SocietyRegistrationPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class SelectSocietyPage() extends Page {
  type PageType = SelectSocietyPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class EventsPage() extends Page {
  type PageType = EventsPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class CreateEventPage(eventWizardStateId: String = "new")
    extends Page {
  type PageType = CreateEventPage
  type Args     = String
  val args: String = eventWizardStateId
}

sealed case class EventWizardStartPage() extends Page {
  type PageType = EventWizardStartPage
  type Args     = Unit
  val args: Unit = ()
}

sealed case class EventWizardStatePage(
    wizardId: String,
    wizardStateId: String
) extends Page {
  type PageType = EventWizardStatePage
  type Args     = (String, String)
  val args: Args = (wizardId, wizardStateId)
}

sealed case class EventWizardQuestionStatePage(
    wizardId: String,
    wizardStateId: String,
    questionId: String
) extends Page {
  type PageType = EventWizardQuestionStatePage
  type Args     = (String, String, String)
  val args: Args = (wizardId, wizardStateId, questionId)
}
sealed case class EventWizardQuestionFormPage(
    wizardId: String,
    wizardStateId: String,
    questionId: String,
    formId: String
) extends Page {
  type PageType = EventWizardQuestionFormPage
  type Args     = (String, String, String, String)
  val args: Args = (wizardId, wizardStateId, questionId, formId)
}

sealed case class EventWizardSubmitPage(
    wizardId: String,
    wizardStateId: String
) extends Page {
  type PageType = EventWizardSubmitPage
  type Args     = (String, String)
  val args: Args = (wizardId, wizardStateId)

}

sealed case class AdminLoginPage() extends Page {
  type PageType = AdminLoginPage
  type Args     = Unit
  val args: Args = ()
}
sealed case class AdminMainPage() extends Page {
  type PageType = AdminMainPage
  type Args     = Unit
  val args: Args = ()
}

sealed case class AdminEventSubmissionsPage() extends Page {
  type PageType = AdminEventSubmissionsPage
  type Args     = Unit
  val args: Args = ()
}

sealed case class FormSubmissionDetailsPage(id: String) extends Page {
  type PageType = FormSubmissionDetailsPage
  type Args     = String
  val args: String = id
}

sealed case class EventSubmissionDetailsPage(id: String) extends Page {
  type PageType = EventSubmissionDetailsPage
  type Args     = String
  val args: String = id
}
