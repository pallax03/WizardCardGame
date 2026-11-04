lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, QualityPlugin)
  .settings(
    name := "WizardCardGame",
    scalaVersion := Dependencies.scala3,
    Compile / mainClass := Some("io.github.pallax03.wizard.Main"),
    libraryDependencies ++= Dependencies.libraries
  )
