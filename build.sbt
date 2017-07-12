import SonatypeKeys._

sonatypeSettings

name := "hbase"

organization := "org.apache.spark.sql.execution.datasources"

version := "0.2"

crossScalaVersions := Seq("2.11.8","2.10.5")

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

org.scalastyle.sbt.ScalastylePlugin.Settings

resolvers ++= Seq(
  "mapr-repo" at "http://repository.mapr.com/maven"
)

val sparkVersion = "2.1.0-mapr-1703"
val hbaseVersion = "1.1.8-mapr-1703"
val hadoopVersion = "2.7.0-mapr-1703"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.hbase" % "hbase-common" % hbaseVersion % "provided",
  "org.apache.hbase" % "hbase-client" % hbaseVersion % "provided",
  "org.apache.hbase" % "hbase-server" % hbaseVersion % "provided"
)
/*
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
*/
