package sbtjavap

import sbt._
import Keys._
import complete.DefaultParsers._
import Serialization.Implicits._
import java.io._
import java.util.stream.Collectors
import sbt.complete.Parser
import scala.collection.JavaConverters._
import scala.reflect.NameTransformer
import xsbti.api.{ClassLike, DefinitionType}

object JavapPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  object autoImport {
    lazy val Javap = (config("javap") extend Compile).hide
    lazy val javap = inputKey[Unit]("Run javap on the given class")
    lazy val javapOpts = settingKey[List[String]]("Options to pass to javap")
    lazy val javapTargetDirectory = settingKey[File]("Where to put decompiled bytecode")
    lazy val javapPager = settingKey[String]("Pager to use for javap")
    lazy val javapClassNames = taskKey[Seq[String]]("")
  }

  import autoImport._

  override def trigger = allRequirements

  private[this] val defaultParser = Space ~> token(StringBasic, "<class name>")

  private[this] def createParser(classNames: Seq[String]): Parser[String] = {
    classNames match {
      case Seq() =>
        defaultParser
      case _ =>
        val other = Space ~> token(StringBasic, _ => true)
        (Space ~> classNames.distinct.map(token(_)).reduce(_ | _)) | other
    }
  }

  override lazy val projectSettings =
    inConfig(Javap)(Defaults.configSettings) ++
      Seq(
        javapClassNames := Tests.allDefs((compile in Compile).value).collect{
          case c: ClassLike =>
            val decoded = c.name.split('.').map(NameTransformer.decode).mkString(".")
            c.definitionType match {
              case DefinitionType.Module =>
                decoded + "$"
              case _ =>
                decoded
            }
        },
        javapClassNames := (javapClassNames storeAs javapClassNames triggeredBy (compile in Compile)).value,
        javapOpts := List("-private", "-c"),
        javapPager := "less",
        javapTargetDirectory := crossTarget.value / "javap",
        javap := InputTask.createDyn(
          Defaults.loadForParser(javapClassNames)(
           (state, classes) => classes.fold(defaultParser)(createParser)
          )
        ){
          Def.task{
            val loader = (testLoader in Test).value
            val r      = (runner in (Javap, run)).value
            val cp     = (fullClasspath or (fullClasspath in Runtime)).value
            val pager      = (javapPager in Javap).value
            val opts   = (javapOpts in Javap).value
            val s      = streams.value
            (cls: String) => {
              val clazz = cls.split('.').map(NameTransformer.encode).mkString(".")
              val dir   = javapTargetDirectory.value // output root
              Def.task(runJavap(s, r, clazz, dir, cp, pager, opts))
            }
          }
        }.evaluated
      )

  def readAll(is: InputStream): String =
    new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"))

  def runJavap(streams: TaskStreams, r: ScalaRun, cls: String, dir: File, cp: Classpath, pager: String, opts: List[String]): Unit = {
    val jars = cp.map(_.data.toString).mkString(":")
    val args = List("javap", "-classpath", jars) ::: opts ::: List(cls)
    dir.mkdirs()
    val dest = dir / s"$cls.bytecode"

    val pb = new ProcessBuilder(args.asJava)
    val proc = pb.start()
    val output = readAll(proc.getInputStream())
    val errors = readAll(proc.getErrorStream())
    val retval = proc.waitFor()

    if (retval == 0) {
      println(s"decompiling $cls to $dest")
      val pw = new PrintWriter(dest)
      pw.print(output)
      pw.close()
      loadInPager(pager, dest)
    } else {
      println(errors)
    }
  }

  def loadInPager(pager: String, file: File): Unit = {
    import java.lang.ProcessBuilder
    val pb = new ProcessBuilder(pager, file.toString)
    pb.inheritIO()
    val p = pb.start()
    p.waitFor()
  }
}
