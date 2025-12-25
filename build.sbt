name := "scalaproject"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  guice,
  "org.playframework" %% "play-ahc-ws" % "3.0.9",                     // <--- WS added
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "com.typesafe.slick" %% "slick" % "3.5.2",
  "org.postgresql" % "postgresql" % "42.7.3"
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
