package assets

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.mozilla.javascript.*

/*
 * val ctx = Context.enter()

    try {
      val scope = context.newScope(ctx)

      context.getScope().get("CoffeeScript",context.getScope()) match {
        case coffee:Scriptable => {
          coffee.get("compile",coffee) match {
            case f:Function => {
              f.call(ctx,scope,coffee,Array(source)).toString()
            }
          }
        }
      }
    } finally {
      Context.exit()
    }
  }
 */

class AssetsPlugin implements Plugin<Project> {
  ScriptableObject scope;

  // AssetsPlugin() {
    /*def ctx = Context.enter()

    def scope = ctx.initStandardObjects()
    def jsStdout = Context.javaToJS(System.out,scope)
    ScriptableObject.putProperty(scope,"stdout",jsStdout)
    def jsStderr = Context.javaToJS(System.err,scope)
    ScriptableObject.putProperty(scope,"stderr",jsStderr)
    def jsSystem = Context.javaToJS(new System(),scope)
    ScriptableObject.putProperty(scope,"system",jsSystem)*/

    /*ctx.evaluateReader(scope,
      new InputStreamReader(this.getClass().getClassLoader()
        .getResourceAsStream("src/resources/coffee-script.js")),
      "coffee",0,null)*/

    // this.scope = scope

    // Context.exit()
  // }

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