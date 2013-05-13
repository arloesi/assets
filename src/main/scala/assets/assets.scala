package assets

import org.gradle.api._
import org.gradle.api.plugins._
import org.mozilla.javascript._

class Plugin extends org.gradle.api.Plugin[Project] {
  val coffee = new Coffee(new Context())
  val less = new Less()

  def apply(project:Project) {
    project.getExtensions().create("assets",classOf[Extensions])
    // project.extensions.create("assets", AssetsExtension)

    // project.task("assets") << {
    //  println("assets: "+project.assets.src)
    // }
  }
}

class Extensions {
  var src = "assets"
}