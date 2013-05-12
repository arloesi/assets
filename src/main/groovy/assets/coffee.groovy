package assets

import org.mozilla.javascript.*

class Coffee {
  Module module

  Coffee(Module module) {
    this.module = module
    module.evaluateFile("src/main/resources/assets/coffee-script.js")
  }

  String compile(String name, String source) {
    def coffee = module.scope.get("CoffeeScript")
    Function compile = coffee.get("compile")

    def ctx = Context.enter()
    def js = compile.call(ctx, module.scope, coffee, source)
    Context.exit()

    return js
  }
}