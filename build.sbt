val AkkaVersion = "2.7.0"
val AlpakkaKafkaVersion = "4.0.0"
val SparkVersion = "3.2.3"
val KafkaVersion = "3.3.1"

Global / scalaVersion := "2.13.10"
Global / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(producer, sparkConsumer)

lazy val api = (project in file("api"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    name := "example-api",
    organization := "com.github.gcnyin",
    libraryDependencies ++= Seq()
  )

lazy val producer = (project in file("producer"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    name := "example-producer",
    organization := "com.github.gcnyin",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion
    )
  )
  .dependsOn(api)

lazy val sparkConsumer = (project in file("spark-consumer"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    name := "example-spark-consumer",
    organization := "com.github.gcnyin",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-sql" % SparkVersion,
      "org.apache.spark" %% "spark-streaming" % SparkVersion % "provided",
      "com.thesamet.scalapb" %% "sparksql32-scalapb0_11" % "1.0.1"
    ),
    assembly / assemblyShadeRules := Seq(
      ShadeRule.rename("com.google.protobuf.**" -> "shadeproto.@1").inAll,
      ShadeRule.rename("scala.collection.compat.**" -> "shadecompat.@1").inAll,
      ShadeRule.rename("shapeless.**" -> "shadeshapeless.@1").inAll
    )
  )
  .dependsOn(api)
