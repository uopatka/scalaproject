name := "scalaproject"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  guice,
  "org.playframework" %% "play-ahc-ws" % "3.0.9",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "com.typesafe.slick" %% "slick" % "3.6.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.6.1",
  "org.postgresql" % "postgresql" % "42.6.0"
)
