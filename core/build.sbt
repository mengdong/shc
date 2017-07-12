
// name := "core"
name := "hbase"

organization := "org.apache.spark.sql.execution.datasources"

resolvers ++= Seq(
  "mapr-repo" at "http://repository.mapr.com/maven"
)

scalaVersion := "2.11.8"

val sparkVersion = "2.1.0-mapr-1703"
val hbaseVersion = "1.1.8-mapr-1703"
val hadoopVersion = "2.7.0-mapr-1703"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.hbase" % "hbase-common" % hbaseVersion % "provided",
  "org.apache.hbase" % "hbase-client" % hbaseVersion % "provided",
  "org.apache.hbase" % "hbase-server" % hbaseVersion % "provided",
  // for tests
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.apache.hbase" % "hbase-common" % hbaseVersion % "test" classifier "tests",
  "org.apache.hbase" % "hbase-server" % hbaseVersion % "test" classifier "tests" exclude("org.mortbay.jetty", "servlet-api-2.5"),
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" exclude("javax.servlet", "servlet-api"),
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests"
)
