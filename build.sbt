import com.typesafe.sbt.packager.docker.DockerChmodType

name := """AccountingApp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.18.0"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.14.1"
libraryDependencies += "org.postgresql" % "postgresql" % "42.3.0"
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.13"
libraryDependencies += "org.scalaj" % "scalaj-http_2.11" % "2.3.0"
libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-RC1"
libraryDependencies += "org.json" % "json" % "20160810"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

Compile/unmanagedSourceDirectories += baseDirectory.value / "../snakked/base/src/main/scala"
Compile/unmanagedSourceDirectories += baseDirectory.value / "../snakked/core/src/main/scala"

dockerBaseImage := "openjdk:21-oracle"

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/def/null"
)

dockerChmodType := DockerChmodType.UserGroupWriteExecute

