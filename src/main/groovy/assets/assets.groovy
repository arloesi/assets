package assets

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.mozilla.javascript.*


class AssetsPlugin implements Plugin<Project> {
  Coffee coffee = new Coffee()
  Less less = new Less()

  void apply(Project project) {
    project.extensions.create("assets", AssetsExtension)

    project.task("assets") << {
      println("assets: "+project.assets.src)
    }
  }
}

class AssetsExtension {
  def String src = "assets"
}