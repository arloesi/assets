package assets

import org.mozilla.javascript._

class Coffee(module:Module) {
  module.evaluateFile("assets/coffee.js")

  def compile(name:String, source:String) = {
    val coffee = module.get("CoffeeScript")
      .asInstanceOf[Scriptable]

    val compile = coffee.get("compile",coffee)
      .asInstanceOf[Function]

    val ctx = Context.enter()
    val js = compile.call(ctx, module.scope, coffee, Array(source))
    Context.exit()

    js
  }
}