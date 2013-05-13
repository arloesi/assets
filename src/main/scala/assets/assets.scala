package assets

import java.util.{List}
import scala.collection.JavaConversions._

import org.gradle.api._
import org.gradle.api.plugins._
import org.gradle.api.tasks._

class Plugin extends org.gradle.api.Plugin[Project] {
  val coffee = new Coffee(new Context())
  val less = new Less()

  def apply(project:Project) {
    println("apply!")
    project.getExtensions().create("assets",classOf[Extensions])
    project.task(Map("type"->classOf[Task]),"assets")
  }
}

class Extensions {
  var optimize = false
  var target = "assets"
}

class Task extends DefaultTask {
  @TaskAction
  def compile() {
    println("compile")
  }
}