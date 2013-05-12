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
    ctx.setOptimizationLevel(0)
    ctx.evaluateReader(scope,reader,source,0,null)
    Context.exit()
  }

  void evaluateFile(String source) {
    def resource = this.getClass().getClassLoader().getResourceAsStream(source)
    def stream = new InputStreamReader(resource)
    evaluateReader(source,stream)
  }

  void evaluateString(String source) {
    evaluateReader(null,new StringReader(source))
  }
}