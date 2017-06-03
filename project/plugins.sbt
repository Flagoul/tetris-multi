// Comment to get more information during initialization
logLevel := Level.Warn

// Sbt plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.13")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")


// scalajs plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.4")
