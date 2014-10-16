name := "play-paypal"

organization := "play-infra"

version := "0.2-SNAPSHOT"

libraryDependencies ++= Seq(
  ws, cache,
  "com.github.alari" %% "wscalacl" % "0.1-SNAPSHOT"
)

crossScalaVersions := Seq("2.10.4", "2.11.3")

lazy val root = (project in file(".")).enablePlugins(play.PlayScala)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

publishTo := Some(Resolver.file("file",  new File( "/mvn-repo" )) )

testOptions in Test += Tests.Argument("junitxml")

resolvers +=  "quonb" at "http://repo.quonb.org/"
