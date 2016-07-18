
val baseSettings = Seq(
  organization := "com.ruiandrebatista",
  version := "0.3.2",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-Ywarn-dead-code",
    "-language:_",
    "-target:jvm-1.7",
    "-encoding", "UTF-8"),
  scalacOptions in Test ++= Seq("-Yrangepos"),
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))

lazy val root = (project in file("."))
  .settings(publish := {})
  .aggregate(scalaPaypalClient, scalaPaypalObjects)

lazy val scalaPaypalClient = (project in file("paypal-client"))
  .settings(baseSettings:_*)
  .settings(
    name := "scalapaypalclient",
    libraryDependencies ++= Seq(akkaActor, akkaHttpCore, akkaHttp, playJson, akkaHttpPlayJson, specs2 % "test"))
  .dependsOn(scalaPaypalObjects)

lazy val scalaPaypalObjects = (project in file("paypal-objects"))
  .settings(baseSettings:_*)
  .settings(name := "scalapaypalobjects",
  libraryDependencies ++= Seq(playJson % "provided", joda, jodaConvert))

// Dependencies

val akkaV = "2.4.5"
val akkaHttpV = "2.0.3"
val akkaActor = "com.typesafe.akka" %% "akka-actor" %akkaV
val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpV
val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpV
val playJson = "com.typesafe.play" %% "play-json" % "2.5.3"
val akkaHttpPlayJson = "de.heikoseeberger" %% "akka-http-play-json" % "1.6.0"
val specs2 = "org.specs2" %% "specs2-core" % "3.7.2"
val joda = "joda-time" % "joda-time" % "2.9.3"
val jodaConvert = "org.joda" % "joda-convert" % "1.8.1"
