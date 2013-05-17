package assets.test

import java.io._
import java.util.{List}
import scala.collection.JavaConversions._

import org.junit._
import org.scalatest.junit._
import org.gradle.{GradleLauncher,StartParameter}
import org.gradle.testfixtures._
import org.gradle.api.{Project}
import org.gradle.api.logging.{StandardOutputListener}
import org.gradle.api.internal.AbstractTask
import assets._

class Assets extends AssertionsForJUnit {
  @Test
  def apply() {
    val project = ProjectBuilder.builder().build()
    project.apply(Map("plugin"->"assets"))
    // assert(project.getTasks().getByName("assets").isInstanceOf[AssetsTask])
  }

  @Test
  def build() {
    val project = ProjectBuilder.builder().build()
    project.apply(Map("plugin"->"assets"))

    project.getTasksByName("assets", false) match {
      case task:AbstractTask => task.execute()
    }
  }
}