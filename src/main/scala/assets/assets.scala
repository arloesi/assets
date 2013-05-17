package assets

import java.io._
import java.util.{List,LinkedList,LinkedHashMap,HashMap,HashSet}
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

  def scriptPath(script:String) = {
    TEMP+"/scripts/"+FilenameUtils.getName(script)+".js"
  }

  def stylePath(script:String) = {
    TEMP+"/styles/"+FilenameUtils.getName(script)+".css"
  }

  def imagePath(image:String) = {
    "assets/images/"+image
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
      IOUtils.toString(getClass().getClassLoader().getResourceAsStream(source.getPath()))
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
        new File(getProject().getBuildDir()+"/"+scriptPath(i)))))

      module.inlines.foreach(script.append _)

      val style = new StringBuilder()
      bundle.styles_r(i => style.append(FileUtils.readFileToString(
        new File(getProject().getBuildDir()+"/"+stylePath(i)))))

      val scriptName = DigestUtils.md5(script.toString()).toString()
      FileUtils.writeStringToFile(new File(baseDir+"/scripts/"+bundle.name+".js"), script.toString())

      val styleName = DigestUtils.md5(style.toString()).toString()
      FileUtils.writeStringToFile(new File(baseDir+"/styles/"+bundle.name+".css"), style.toString())
      FileUtils.writeStringToFile(new File(baseDir+"/modules/"+bundle.name+".html"), module.master(bundle,scriptName,styleName))
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
                  images_r(i => main.dependsOn(i._2))
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
      val task = buildCopyTask(project,s,imagePath(t))
      tasks.add(task)
    }

    tasks
  }

  def buildScriptTasks(project:Project,scripts:List[String]) = {
    val tasks = new LinkedList[Task]()

    for(source <- scripts) {
      val target = scriptPath(source)
      val ext = FilenameUtils.getExtension(source)

      if(ext == "js") {
        tasks.add(buildCopyTask(project,source,target))
      } else {
        tasks.add(buildCoffeeTask(project,source,target))
      }
    }

    tasks
  }

  def buildStyleTasks(project:Project,styles:List[String]) = {
    val tasks = new LinkedList[Task]()

    for(source <- styles) {
      val target = stylePath(source)
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

    bundle.bundles_r(x => if(x != bundle) task.getInputs().file(project.getBuildDir()+"/"+scriptPath(x.name+".js")))
    bundle.scripts_r(x => task.getInputs().file(project.getBuildDir()+"/"+scriptPath(x)))
    bundle.styles_r(x => task.getOutputs().file(project.getBuildDir()+"/"+stylePath(x)))

    task
  }

  def buildFileTask(project:Project,`type`:Class[_],source:String,target:String) = {
    val task = project.task(Map("type"->`type`),target)
    task.getInputs().file(source)
    task.getOutputs().file(project.getBuildDir().getPath()+"/"+target)
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
