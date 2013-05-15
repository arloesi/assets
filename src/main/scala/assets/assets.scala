package assets

import java.io._
import java.util.{List,LinkedList,LinkedHashMap}
import scala.collection.JavaConversions._
import groovy.util.ConfigSlurper

import org.apache.commons.io._

import org.gradle.api.{Project,DefaultTask,Task}
import org.gradle.api.plugins._
import org.gradle.api.tasks._

class Plugin extends org.gradle.api.Plugin[Project] {
  val coffee = new Coffee(new Context())
  val less = new Less()

  def apply(project:Project) {
    project.getExtensions().create("assets",classOf[Extension])
    val config = new ConfigSlurper().parse(getClass().getClassLoader().getResource("assets.gradle"))
    val tasks = new LinkedList[Task]()

    def parseImages(v:Object) = {
      val tasks = new LinkedList[Task]()
      val images = Bundle.loadImages(v.asInstanceOf[java.util.Map[String,Object]])

      for((s,t) <- images) {
        val task = project.task(Map("type"->classOf[ImageTask]),t)
        task.getInputs().file(s)
        task.getOutputs().file(t)
        tasks.add(task)
      }

      tasks
    }

    config.getProperty("bundles") match {
      case null => ()
      case bundles:java.util.Map[String,Object] => {
        for((k,v) <- bundles) {
          tasks.addAll(parseImages(v))
        }
      }
    }

    config.getProperty("modules") match {
      case null => ()
      case modules:java.util.Map[String,Object] => {
        for((k,v) <- modules) {
          tasks.addAll(parseImages(v))
        }
      }
    }

    val main = project.task(Map("type"->classOf[AssetsTask]),"assets")
    tasks.foreach(main.getDependsOn().add _)
  }
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
    val extension = getProject().getExtensions().getByName("assets").asInstanceOf[Extension]
    println("optimize: "+extension.getOptimize())
  }
}

class ImageTask extends DefaultTask {
  @TaskAction
  def compile() {
    FileUtils.copyFile(getInputs().getFiles().first, getOutputs().getFiles().first)
  }
}