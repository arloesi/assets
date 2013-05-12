package assets

import org.mozilla.javascript.*

class Coffee {
  Module module

  Coffee(Module module) {
    this.module = module
    module.evaluateFile("src/main/resources/coffee-script.js")
  }

  String compile(String name, String source) {
    def coffee = module.get("CoffeeScript")
    Function compile = coffee.get("compile")
    return compile.call(ctx, module.scope, coffee, [source])
  }
}