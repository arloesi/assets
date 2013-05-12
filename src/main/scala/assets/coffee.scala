package assets

import java.io._
import java.util._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.apache.commons.io._
import org.mozilla.javascript._

class Coffee(module:Module) {
  module.evaluateFile("assets/coffee.js")

  def compile(source:String):String = {
    val list = new LinkedList[String]()
    val stream = this.getClass().getClassLoader().getResourceAsStream(source)
    compile(source,IOUtils.toString(stream))
  }

  def compile(name:String, source:String):String = {
    val coffee = module.get("CoffeeScript")
      .asInstanceOf[Scriptable]

    val compile = coffee.get("compile",coffee)
      .asInstanceOf[Function]

    val ctx = Context.enter()
    val js = compile.call(ctx, module.scope, coffee, Array(source))
      .asInstanceOf[String]

    Context.exit()

    js
  }
}