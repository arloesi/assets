package assets

import java.util._
import scala.collection.JavaConversions._
import org.mozilla.javascript._

class Markup(module:Module,source:String) {
  module.evaluateFile(source)

  val scope = {
    val ctx = Context.enter()
    val unwrap:Function = module.get("__markup_unwrap_module")
    val scope = unwrap.call(ctx, module.scope, module.scope,Array()).asInstanceOf[Scriptable]
    Context.exit()
    scope
  }

  val includes = scope.get("includes",scope).asInstanceOf[List[String]]
  val scripts = scope.get("scripts",scope).asInstanceOf[List[String]]
  val inlines = scope.get("inlines",scope).asInstanceOf[List[String]]
  val styles = scope.get("styles",scope).asInstanceOf[List[String]]

  def render() = {
    ""
  }
}