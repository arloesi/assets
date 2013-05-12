package assets

import java.io._
import org.mozilla.javascript._

class Module(val scope:Scriptable) {
  def this() = this({
    val ctx = Context.enter()
    val scope = ctx.initStandardObjects()
    val jsStdout = Context.javaToJS(System.out,scope)
    ScriptableObject.putProperty(scope,"stdout",jsStdout)
    val jsStderr = Context.javaToJS(System.err,scope)
    ScriptableObject.putProperty(scope,"stderr",jsStderr)
    Context.exit()
    scope})

  def this(module:Module) = this({
    val ctx = Context.enter()
    val scope = ctx.newObject(module.scope)
    scope.setParentScope(module.scope)
    Context.exit()
    scope})

  def get[T<:Object](name:String):T = {
    scope.get(name,scope).asInstanceOf[T]
  }

  def evaluateReader(source:String, reader:Reader) {
    val ctx = Context.enter()
    ctx.setOptimizationLevel(-1)
    ctx.evaluateReader(scope,reader,source,0,null)
    Context.exit()
  }

  def evaluateFile(source:String) {
    val resource = this.getClass().getClassLoader().getResourceAsStream(source)
    val stream = new InputStreamReader(resource)
    evaluateReader(source,stream)
  }

  def evaluateString(source:String) {
    evaluateReader(null,new StringReader(source))
  }
}