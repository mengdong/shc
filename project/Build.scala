import sbt._
import Keys._


object ShcBuild extends Build {

    lazy val root = Project(id = "Shc",
        base = file(".")) aggregate(core)

    lazy val core = Project(id = "shcCore",
        base = file("core"))

}