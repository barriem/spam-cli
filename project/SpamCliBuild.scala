import sbt._
import sbt.Keys._

object SpamCliBuild extends Build {

  lazy val spamcli = Project(
    id = "spam-cli",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "spam-cli",
      organization := "org.bcm",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.4",
      resolvers += "MVN Repo" at "http://repo1.maven.org/maven2",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.3.4",
        "com.typesafe.akka" %% "akka-remote" % "2.3.4",
        "org.bcm" %% "spam" % "0.1-SNAPSHOT"
      )
    )
  )
}
