import scala.util.Properties
import com.typesafe.sbt.SbtScalariform.scalariformSettings

name := "salad-metrics-core"

organization := "com.netaporter.salad"

version := "0.1." + Properties.envOrElse("BUILD_NUMBER", "0") + "-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-feature")

resolvers ++= 
  ("NAP repo" at "http://artifactory.dave.net-a-porter.com:8081/artifactory/repo") ::
  Nil

val akka = "2.2.3"
val spray = "1.2.0"
val jackson = "2.2.2"
val metrics = "3.0.1"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akka,
    "com.codahale.metrics" % "metrics-core" % metrics,
    "com.codahale.metrics" % "metrics-json" % metrics,
    "com.fasterxml.jackson.core" % "jackson-databind" % jackson,
    "com.fasterxml.jackson.module" % "jackson-module-afterburner" % jackson,
    "io.spray" % "spray-routing" % spray,
    "io.spray" % "spray-testkit" % spray % "test",
    "org.scalatest" %% "scalatest" % "2.0" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akka % "test",
//    "com.chuusai" % "shapeless" % "2.0.0-M1" % "test" cross CrossVersion.full,
    "org.specs2" %% "specs2-core" % "2.3.7" % "test"
)

scalariformSettings

// Generate junit xml reports
testOptions <+= (target in Test) map { t =>
  Tests.Argument("-o", "-u", t + "/test-reports")
}

publishTo := Some(Resolver.file("file", new File("target/publish")))
