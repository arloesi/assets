package assets

import java.io._
import java.util.{List,LinkedList,LinkedHashMap,HashMap,HashSet}
import java.security._;
import scala.collection.JavaConversions._
import groovy.util.ConfigSlurper

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.gradle.api.{Project,DefaultTask,Task}
import org.gradle.api.plugins._
import org.gradle.api.tasks._

object Assets {
  import Compiler._

  final val TEMP = "assets-temp/"

  val context = new Context()
  val coffee = new Coffee(context)
  val less = new Less()
  val markup = new Markup(context,coffee)

  def md5(source:String) = {
      val md = MessageDigest.getInstance("MD5");
      val dataBytes = source.getBytes()
      md.update(dataBytes, 0, dataBytes.length);
      val mdbytes = md.digest();

      val sb = new StringBuffer();
      for (i <- 0 until mdbytes.length) {
        sb.append(Integer.toString((mdbytes(i) & 0xff) + 0x100, 16).substring(1));
      }

      sb.toString()
  }

  def sourcePath(source:String) = {
    new File(source).getCanonicalPath()
  }

  def targetPath(project:Project,target:String) = {
    new File(project.getBuildDir().getPath()+"/"+target).getCanonicalPath()
  }

  def scriptPath(script:String) = {
    TEMP+"/scripts/"+FilenameUtils.getPathNoEndSeparator(script)+"/"+FilenameUtils.getBaseName(script)+".js"
  }

  def stylePath(script:String) = {
    TEMP+"/styles/"+FilenameUtils.getPathNoEndSeparator(script)+"/"+FilenameUtils.getBaseName(script)+".css"
  }

  def imagePath(image:String) = {
    "assets/"+image
  }

  class Extension {
    private var optimize = false
    def setOptimize(value:Boolean) {this.optimize=value}
    def getOptimize() = {this.optimize}

    private var target = "assets"
    def setTarget(value:String) {this.target=target}
    def getTarget() = {this.target}
  }

  class AssetsTask extends DefaultTask {
    @TaskAction
    def compile() {
    }
  }

  abstract class FileTask extends DefaultTask {
    lazy val source = getInputs().getFiles().first
    lazy val target = getOutputs().getFiles().first

    @TaskAction
    def compile()

    def load() = {
      FileUtils.readFileToString(source)
    }

    def save(source:String) {
      FileUtils.writeStringToFile(target, source)
    }
  }

  class CopyTask extends FileTask {
    override def compile() {
      FileUtils.copyFile(source, target)
    }
  }

  class CoffeeTask extends FileTask {
    override def compile() {
      save(coffee.compile(source.getPath(),load()))
    }
  }

  class LessTask extends FileTask {
    override def compile() {
      save(less.compile(source.getPath(),load()))
    }
  }

  class ModuleTask extends DefaultTask {
    var bundle:Bundle = null

    @TaskAction
    def compile() {
      val baseDir = getProject().getBuildDir()+"/assets"
      val module = markup.compile(bundle)
      val script = new StringBuilder()

      bundle.scripts_r(i => script.append(FileUtils.readFileToString(
        new File(targetPath(getProject(),scriptPath(i._2))))))

      module.inlines.foreach(script.append _)

      val style = new StringBuilder()
      bundle.styles_r(i => style.append(FileUtils.readFileToString(
        new File(targetPath(getProject(),stylePath(i._2))))))

      val scriptName = md5(script.toString()).toString()
      FileUtils.writeStringToFile(new File(baseDir+"/"+bundle.name+".js"), script.toString())

      val styleName = md5(style.toString()).toString()
      FileUtils.writeStringToFile(new File(baseDir+"/"+bundle.name+".css"), style.toString())
      FileUtils.writeStringToFile(new File(getProject().getBuildDir()+"/modules/"+bundle.name+".html"), module.master(bundle,scriptName,styleName))
    }
  }
}

class Assets extends org.gradle.api.Plugin[Project] {
  import Assets._

  def apply(project:Project) {
    val factory = new HashMap[String,Bundle]()
    val bundles = new LinkedList[Bundle]()
    val imports = new HashSet[String]()
    val main = project.task(Map("type"->classOf[AssetsTask]),"assets")

    def parse(source:String,root:Boolean) {
      val config = new ConfigSlurper().parse(FileUtils.readFileToString(new File(source+"/assets.gradle")))

      config.getProperty("imports") match {
        case list:List[String] => list.foreach(imports.add _)
        case _ => ()
      }

      config.getProperty("include") match {
        case list:List[String] => list.foreach(i => parse(i,false))
        case _ => ()
      }

      config.getProperty("bundles") match {
        case null => ()
        case modules:java.util.Map[String,Object] => {
          for((k,v) <- modules) {
            val bundle = new Bundle(factory,k,source,v.asInstanceOf[java.util.Map[String,Object]]) {
              override def initialize() {
                buildImageTasks(project,images)
                buildScriptTasks(project,scripts)
                buildStyleTasks(project,styles)
              }
            }

            factory.put(bundle.name, bundle)
            bundles.add(bundle)
          }
        }
      }

      config.getProperty("modules") match {
        case null => ()
        case modules:java.util.Map[String,Object] => {
          for((k,v) <- modules) {
            if(root || imports.contains(k)) {
              val bundle = new Bundle(factory,k,source,v.asInstanceOf[java.util.Map[String,Object]]) {
                override def initialize() {
                  buildImageTasks(project,images)
                  buildScriptTasks(project,scripts)
                  buildStyleTasks(project,styles)
                  buildModuleTask(project,this)

                  main.dependsOn(name)
                  images_r(i => main.dependsOn(targetPath(project,imagePath(i._2))))
                }
              }

              bundles.add(bundle)
            }
          }
        }
      }
    }

    parse(".",true)
    bundles.foreach(i => i.initialize())
  }

  def buildImageTasks(project:Project,images:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((s,t) <- images) {
      val task = buildCopyTask(project,sourcePath(s),imagePath(t))
      tasks.add(task)
    }

    tasks
  }

  def buildScriptTasks(project:Project,scripts:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((source,targ) <- scripts) {
      val target = scriptPath(targ)
      val ext = FilenameUtils.getExtension(source)

      if(ext == "js") {
        tasks.add(buildCopyTask(project,source,target))
      } else {
        tasks.add(buildCoffeeTask(project,source,target))
      }
    }

    tasks
  }

  def buildStyleTasks(project:Project,styles:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((source,targ) <- styles) {
      val target = stylePath(targ)
      val ext = FilenameUtils.getExtension(source)

      if(ext == "css") {
        tasks.add(buildCopyTask(project,source,target))
      } else {
        tasks.add(buildLessTask(project,source,target))
      }
    }

    tasks
  }

  def buildModuleTask(project:Project,bundle:Bundle) {
    val task = project.task(Map("type"->classOf[ModuleTask]),bundle.name).asInstanceOf[ModuleTask]
    task.bundle = bundle

    bundle.scripts_r(x => {
      val path = targetPath(project,scriptPath(x._2))
      task.dependsOn(path)
      task.getInputs().file(path)
    })

    bundle.styles_r(x => {
      val path = targetPath(project,stylePath(x._2))
      task.dependsOn(path)
      task.getInputs().file(path)
    })

    task.getInputs().file(sourcePath(bundle.source+"/modules/"+bundle.name+".coffee"))
    task.getOutputs().file(targetPath(project,"scripts/"+bundle.name+".js"))
    task.getOutputs().file(targetPath(project,"styles/"+bundle.name+".css"))
    task.getOutputs().file(targetPath(project,"modules/"+bundle.name+".html"))

    task
  }

  def buildFileTask(project:Project,`type`:Class[_],source:String,target:String) = {
    val task = project.task(Map("type"->`type`),targetPath(project,target))
    task.getInputs().file(new File(source).getCanonicalPath())
    task.getOutputs().file(targetPath(project,target))
    task
  }

  def buildCopyTask(project:Project,source:String,target:String) = {
    buildFileTask(project,classOf[CopyTask],source,target)
  }

  def buildCoffeeTask(project:Project,source:String,target:String) = {
    buildFileTask(project,classOf[CoffeeTask],source,target)
  }

  def buildLessTask(project:Project,source:String,target:String) = {
    buildFileTask(project,classOf[LessTask],source,target)
  }
}
