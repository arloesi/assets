package assets.test

// import java.util._
import scala.collection.JavaConversions._

import org.junit._
import org.scalatest.junit._
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import assets._

class Assets extends AssertionsForJUnit {
  @Test
  def apply() {
    val project = ProjectBuilder.builder().build()
    project.apply(Map("plugin"->"assets"))
    assert(project.getTasks().getByName("assets").isInstanceOf[Task])
  }
}