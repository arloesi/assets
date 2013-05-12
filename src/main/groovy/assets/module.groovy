package assets

import org.mozilla.javascript.*

class Module {
  ScriptableObject scope;

  Module() {
    def ctx = Context.enter()

    def scope = ctx.initStandardObjects()
    def jsStdout = Context.javaToJS(System.out,scope)
    ScriptableObject.putProperty(scope,"stdout",jsStdout)
    def jsStderr = Context.javaToJS(System.err,scope)
    ScriptableObject.putProperty(scope,"stderr",jsStderr)
    // def jsSystem = Context.javaToJS(new System(),scope)
    // ScriptableObject.putProperty(scope,"system",jsSystem)

    /*ctx.evaluateReader(scope,
      new InputStreamReader(this.getClass().getClassLoader()
        .getResourceAsStream("src/main/resources/coffee-script.js")),
      "coffee",0,null)*/

    this.scope = scope

    Context.exit()
  }

  Module(Scriptable scope) {
    def ctx = Context.enter()
    this.scope = ctx.newObject(scope)
    this.scope.setParentScope(scope)
    ctx.exit()
  }

  Module(Module module) {
    this(module.scope)
  }

  void evaluateReader(String source, Reader reader) {
    def ctx = Context.enter()
    ctx.evaluateReader(scope,reader,source,0,null)
    Context.exit()
  }

  void evaluateFile(String source) {
    evaluateReader(source,new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(source)))

    /*def ctx = Context.enter()

    ctx.evaluateReader(scope,
      new InputStreamReader(this.getClass().getClassLoader()
        .getResourceAsStream(source)),
      source,0,null)

    Context.exit()*/
  }

  void evaluateString(String source) {
    evaluateReader(null,new StringReader(source))
  }
}