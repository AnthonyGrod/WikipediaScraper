scalaVersion := "2.13.8"

ThisBuild / scalaVersion := "2.13.10"

name := "wikipedia-scraper"
organization := "ch.epfl.scala"
version := "1.0"


libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"


lazy val root = (project in file("."))
  .settings(
    name := "scala-web-scraping",
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.15.3",
      "net.ruippeixotog" %% "scala-scraper" % "3.0.0",
      "org.seleniumhq.selenium" % "selenium-java" % "4.5.0",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
      "com.softwaremill.sttp.client3" %% "core" % "3.3.15",
      "com.lihaoyi" %% "upickle" % "0.9.5",
      "com.lihaoyi" %% "ujson" % "1.4.0",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    )
  )
