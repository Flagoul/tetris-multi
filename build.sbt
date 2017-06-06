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
      "mysql" % "mysql-connector-java" % "5.1.42",
      "com.typesafe.play" %% "play-slick" % "2.1.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "2.1.0",
      "org.mindrot" % "jbcrypt" % "0.4",
      "org.scalactic" %% "scalactic" % "3.0.1",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      specs2 % Test
    )
  )
  .enablePlugins(PlayScala)
  .dependsOn(sharedJvm)


lazy val client = (project in file("client"))
  .settings(
    scalaVersion := scalaV,
    scalaJSUseMainModuleInitializer := true,
    resolvers += "mmreleases" at "https://artifactory.mediamath.com/artifactory/libs-release-global",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.mediamath" %%% "scala-json" % "1.0",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
      "org.webjars.npm" % "jquery" % "3.2.1"
    ),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "jquery" % "3.2.1" / "dist/jquery.js",
      "org.webjars.bower" % "datatables" % "1.10.15" / "jquery.dataTables.js" dependsOn "dist/jquery.js",
      "org.webjars.bower" % "datatables" % "1.10.15" / "dataTables.bootstrap4.js" dependsOn "jquery.dataTables.js"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)


lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "org.webjars" % "bootstrap" % "4.0.0-alpha.6" exclude("org.webjars", "jquery"),
      "org.webjars.bower" % "datatables" % "1.10.15" exclude("org.webjars", "jquery")
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)


lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
