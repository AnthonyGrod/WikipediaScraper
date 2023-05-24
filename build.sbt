scalaVersion := "2.13.8"

ThisBuild / scalaVersion := "2.13.10"

name := "wikipedia-scraper"
organization := "ch.epfl.scala"
version := "1.0"


lazy val root = (project in file("."))
  .settings(
    name := "scala-web-scraping",
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.15.3",
      "com.softwaremill.sttp.client3" %% "core" % "3.3.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    )
  )
