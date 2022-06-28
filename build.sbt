ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

val Http4sVersion = "0.23.12"
val Json4SVersion = "4.0.5"
val LogbackVersion = "1.2.11"
val CirceVersion = "0.14.2"
val AkkaVersion = "2.6.9"
val AkkaHttpVersion = "10.2.8"
val AkkaCassandraVersion = "1.0.5"
val JsonValidatorVersion = "2.2.14"
val ScalaTestVersion = "3.2.12"
val AkkaHTTPCirce = "1.39.2"

lazy val root = (project in file("."))
  .settings(
    name := "schema-registry",
    libraryDependencies ++= Seq(
      "org.json4s"                 %% "json4s-native"              % Json4SVersion,
      "org.json4s"                 %% "json4s-jackson"             % Json4SVersion,
      "io.circe"                   %% "circe-generic"              % CirceVersion,
      "io.circe"                   %% "circe-literal"              % CirceVersion,
      "com.github.java-json-tools" % "json-schema-validator"       % JsonValidatorVersion,
      "com.typesafe.akka"          %% "akka-http"                  % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-actor-typed"           % AkkaVersion,
      "com.typesafe.akka"          %% "akka-stream"                % AkkaVersion,
      "com.typesafe.akka"          %% "akka-persistence-typed"     % AkkaVersion,
      "com.typesafe.akka"          %% "akka-persistence-cassandra" % AkkaCassandraVersion,
      "de.heikoseeberger"          %% "akka-http-circe"            % AkkaHTTPCirce,
      "ch.qos.logback"             % "logback-classic"             % LogbackVersion,
      "com.typesafe.akka"          %% "akka-persistence-testkit"   % AkkaVersion % Test,
      "org.scalatest"              %% "scalatest"                  % ScalaTestVersion % Test,
      "com.typesafe.akka"          %% "akka-actor-testkit-typed"   % AkkaVersion % Test
    ),
    Test / parallelExecution := false
  )

// Format *.sbt and project/*.scala files, main sources and test sources
addCommandAlias("fmt-format", "all scalafmtSbt scalafmt test:scalafmt")
// Check *.sbt and project/*.scala files, main sources and test sources for formatting
addCommandAlias("fmt-check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
