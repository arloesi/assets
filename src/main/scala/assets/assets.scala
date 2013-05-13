package assets

import java.util.{List,LinkedHashMap}
import scala.collection.JavaConversions._

import org.gradle.api._
import org.gradle.api.plugins._
import org.gradle.api.tasks._

class Plugin extends org.gradle.api.Plugin[Project] {
  val coffee = new Coffee(new Context())
  val less = new Less()

  def apply(project:Project) {
    println("apply!")
    project.getExtensions().create("assets",classOf[Extension])
    project.task(Map("type"->classOf[Task]),"assets")
  }
}

class Extension {
  private var optimize = false
  def setOptimize(value:Boolean) {this.optimize=value}
  def getOptimize() = {this.optimize}

  private var target = "assets"
  def setTarget(value:String) {this.target=target}
  def getTarget() = {this.target}

  private var bundles:java.util.Map[String,Object]) = new LinkedHashMap[String,Object]()
  def setBundles(bundles:Map[String,Object]) {this.bundles=bundles}
  def getBundles():Map[String,Object]) = {this.bundles}
}

class Task extends DefaultTask {
  @TaskAction
  def compile() {
    val extension = getProject().getExtensions().getByName("assets").asInstanceOf[Extension]
    println("optimize: "+extension.getOptimize())
    println("target: "+extension.getTarget())
    println("bundles: "+extension.getBundles().size())
  }
}