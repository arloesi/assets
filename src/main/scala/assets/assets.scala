package assets

import java.io._
import java.util.{List,LinkedList,LinkedHashMap}
import scala.collection.JavaConversions._
import groovy.util.ConfigSlurper

import org.apache.commons.io._

import org.gradle.api.{Project,DefaultTask,Task}
import org.gradle.api.plugins._
import org.gradle.api.tasks._

object Assets {
  import Compiler._

  val context = new Context()
  val coffee = new Coffee(context)
  val less = new Less()
  val markup = new Markup(context,coffee)

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
      val extension = getProject().getExtensions().getByName("assets").asInstanceOf[Extension]
      println("optimize: "+extension.getOptimize())
    }
  }

  class FileTask extends DefaultTask {
    lazy val source = getInputs().getFiles().first
    lazy val target = getOutputs().getFiles().first

    @TaskAction
    def compile()

    def load() = {
      IOUtils.toString(getClass().getClassLoader().getResourceAsStream(source.getPath()))
    }

    def save(source:String) {
      FileUtils.write(target,source)
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
}

class Assets extends org.gradle.api.Plugin[Project] {
  import Assets._

  def apply(project:Project) {
    project.getExtensions().create("assets",classOf[Extension])
    val config = new ConfigSlurper().parse(getClass().getClassLoader().getResource("assets.gradle"))
    val tasks = new LinkedList[Task]()

    config.getProperty("modules") match {
      case null => ()
      case modules:java.util.Map[String,Object] => {
        for((k,v) <- modules) {
          val bundle = new Bundle(k,v.asInstanceOf[java.util.Map[String,Object]])

          buildImageTasks(project,bundle.images)
          buildScriptTasks(project,bundle.scripts)
          buildStyleTasks(project,bundle.styles)

          // module
          buildScriptTask(project,bundle)
          buildStyleTask(project,bundle)
          buildMarkupTask(project,bundle)
        }
      }
    }

    val main = project.task(Map("type"->classOf[AssetsTask]),"assets")
    tasks.foreach(main.getDependsOn().add _)
  }

  def buildImageTasks(project:Project,images:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((s,t) <- images) {
      val task = buildCopyTask(project,s,t)
      tasks.add(task)
    }

    tasks
  }

  def buildScriptTasks(project:Project,scripts:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((s,t) <- scripts) {
      val ext = FilenameUtils.getExtension(s)

      if(ext == "js") {
        tasks.add(buildCopyTask(project,s,t))
      } else {
        tasks.add(buildCoffeeTask(project,s,t))
      }
    }

    tasks
  }

  def buildStyleTasks(project:Project,styles:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((s,t) <- styles) {
      val ext = FilenameUtils.getExtension(s)

      if(ext == "css") {
        tasks.add(buildCopyTask(project,s,t))
      } else {
        tasks.add(buildLessTask(project,s,t))
      }
    }

    tasks
  }

  def buildFileTask(project:Project,`type`:Class[_],source:String,target:String) = {
    val task = project.task(Map("type"->`type`),target)
    task.getInputs().file(source)
    task.getOutputs().file(target)
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

  def buildScriptTask(project:Project,bundle:Bundle) {

  }

  def buildStyleTask(project:Project,bundle:Bundle) {

  }

  def buildMarkupTask(project:Project,bundle:Bundle) {

  }
}
