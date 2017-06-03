// Original source : https://github.com/vmunier/play-with-scalajs-example
val scalaV = "2.11.11"


lazy val server = (project in file("server"))
  .settings(
    scalaVersion := scalaV,
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.0.0",
      "mysql" % "mysql-connector-java" % "5.1.39",
      "com.typesafe.play" %% "play-slick" % "2.1.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "2.1.0",
      "org.mindrot" % "jbcrypt" % "0.4",
      specs2 % Test
    )
  )
  .enablePlugins(PlayScala)
  .dependsOn(sharedJvm)


lazy val client = (project in file("client"))
  .settings(
    scalaVersion := scalaV,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)


lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := scalaV)
  .jsConfigure(_ enablePlugins ScalaJSWeb)


lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
