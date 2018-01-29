package sbtjavap

import sbt._
import Keys._
import complete.DefaultParsers._
import scala.sys.process.Process

object JavapPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  object autoImport {
    lazy val Javap = (config("javap") extend Compile).hide
    lazy val javap = inputKey[Unit]("Run javap on the given class")
    lazy val javapOpts = settingKey[List[String]]("Options to pass to javap")
    lazy val javapTargetDirectory = settingKey[File]("Where to put decompiled bytecode")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val projectSettings =
    inConfig(Javap)(Defaults.configSettings) ++
      Seq(
        javapOpts := List("-c"),
        javapTargetDirectory := crossTarget.value / "javap",
        javap := {
          val cls   = (Space ~> StringBasic).parsed
          val r     = (runner in (Javap, run)).value
          val dir   = javapTargetDirectory.value // output root
          val cp    = (fullClasspath or (fullClasspath in Runtime)).value
          val opts  = (javapOpts in Javap).value
          runJavap(streams.value, r, cls, dir, cp, opts)
        }
      )

  def runJavap(streams: TaskStreams, r: ScalaRun, cls: String, dir: File, cp: Classpath, opts: List[String]): Unit = {
    val jars = cp.map(_.data.toString).mkString(":")
    val args = List("javap","-classpath", jars) ::: opts ::: List(cls)
    val proc = Process(args)
    dir.mkdirs()
    val dest = dir / s"$cls.bytecode"
    println(s"decompiling $cls to $dest")

    (proc #> dest).run()
  }
}
