package assets

import java.util._
import scala.collection.JavaConversions._
import org.mozilla.javascript._

class Markup(module:Module,source:String) {
  module.evaluateFile(source)

  val ctx = Context.enter()

  val unwrap = module.get("__markup_unwrap_list")
    .asInstanceOf[Function]

  val (includes,scripts,styles,markup) = module.get("module") match {
    case scope:Scriptable => {
      val inc = scope.get("includes",scope) match {
        case includes:Object =>
          unwrap.call(ctx,module.scope,module.scope,Array(includes))
            .asInstanceOf[List[String]]
        case _ => new LinkedList[String]()
      }

      val scr = scope.get("scripts", scope) match {
        case scripts:Object =>
          unwrap.call(ctx,module.scope,module.scope,Array(scripts))
            .asInstanceOf[List[String]]
        case _ => new LinkedList[String]()
      }

      val stl = scope.get("styles", scope) match {
        case styles:Object =>
          unwrap.call(ctx,module.scope,module.scope,Array(styles))
            .asInstanceOf[List[String]]
        case _ => new LinkedList[String]()
      }

      (inc,scr,stl,scope.get("markup",scope))
    }
  }

  Context.exit()


  def render() = {
    ""
  }
}