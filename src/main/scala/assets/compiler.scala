package assets

import java.io._
import java.util._
import scala.collection.JavaConversions._
import org.apache.commons.io._
import org.mozilla.javascript._
import org.lesscss.LessCompiler

object Compiler {
  import Context._

  class Coffee(module:Context) {
    module.evaluateFile("assets/coffee.js")

    override def compile(name:String, source:String):String = {
      val coffee = module.get("CoffeeScript")
        .asInstanceOf[Scriptable]

      val compile = coffee.get("compile",coffee)
        .asInstanceOf[Function]

      withContext(ctx => compile.call(ctx, module.scope, coffee, Array(source)).asInstanceOf[String])
    }
  }

  class Less {
    val less = new LessCompiler()

    override def compile(name:String,source:String):String = {
      less.compile(source)
    }
  }

  class Markup(context:Context,coffee:Coffee) extends Compiler {
    context.evaluateString(coffee.compile("markup",IOUtils.toString(getClass().getClassLoader().getResourceAsStream("assets/markup.coffee"))))

    override def compile(name:String,source:String):( = {
      null
    }
  }
}