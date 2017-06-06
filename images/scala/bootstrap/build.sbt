name := "bootstrap"
version := "1.0"

scalaVersion := "2.11.11"


libraryDependencies ++= Seq(
  // Shared dependencies
  "org.webjars" % "bootstrap" % "4.0.0-alpha.6" exclude("org.webjars", "jquery"),
  "org.webjars.bower" % "datatables" % "1.10.15" exclude("org.webjars", "jquery"),
  // Server dependencies
  "com.vmunier" %% "scalajs-scripts" % "1.0.0",
  "mysql" % "mysql-connector-java" % "5.1.39",
  "com.typesafe.play" %% "play-slick" % "2.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.1.0",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  specs2 % Test,
  // Client dependencies
  "org.scala-js" %%% "scalajs-dom" % "0.9.2",
  "com.mediamath" %%% "scala-json" % "1.0",
  "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
  "org.webjars.npm" % "jquery" % "3.2.1"
)

resolvers += "mmreleases" at "https://artifactory.mediamath.com/artifactory/libs-release-global"


lazy val boostrap = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
