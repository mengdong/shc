import sbt._
import Keys._

object ShcBuild extends Build {

    lazy val root =  project in file(".") aggregate(core) dependsOn(core)

    lazy val core =  project in file("core") 

}
