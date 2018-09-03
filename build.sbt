name := "ferris-http-service-client"

organization := "com.ferris"

version := "0.0.2"

scalaVersion := "2.12.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaHttpV     = "10.0.1"
  val ferrisV       = "0.0.1"
  val scalaLoggingV = "3.8.0"
  val scalaTestV    = "3.0.1"
  val fommilV       = "1.4.0"
  val mockitoV      = "1.10.19"
  Seq(
    "com.typesafe.akka"          %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka"          %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka"          %% "akka-http-testkit"    % akkaHttpV,
    "com.typesafe.scala-logging" %% "scala-logging"        % scalaLoggingV,
    "com.ferris"                 %% "ferris-json-utils"    % ferrisV,
    "com.github.fommil"          %% "spray-json-shapeless" % fommilV,
    "org.scalatest"              %% "scalatest"            % scalaTestV       % Test,
    "org.mockito"                 %  "mockito-all"         % mockitoV         % Test
  )
}
