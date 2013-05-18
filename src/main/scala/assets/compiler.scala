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

    def compile(name:String, source:String):String = {
      val coffee = module.get("CoffeeScript")
        .asInstanceOf[Scriptable]

      val compile = coffee.get("compile",coffee)
        .asInstanceOf[Function]

      withContext(ctx => compile.call(ctx, module.scope, coffee, Array(source)).asInstanceOf[String])
    }
  }

  class Less {
    val less = new LessCompiler()
    val comp = new LessCompiler()
    comp.setCompress(true)

    def compile(name:String,source:String):String = {
      less.compile(source)
    }

    def compress(source:String):String = {
      comp.compile(source)
    }
  }

  class Markup(context:Context,coffee:Coffee) {
    class Module {
      val inlines = new LinkedList[String];
      val markup = new LinkedList[Scriptable];
      var source:Scriptable = null

      def master(bundle:Bundle,script:String,style:String) = {
        if(this.source == null) {
          throw new Exception("source module is null for in master")
        }

        withContext(ctx => render.call(ctx,context.scope,source,Array(bundle.name,script,style,markup)).toString())
      }
    }

    context.evaluateString(coffee.compile("markup",IOUtils.toString(getClass().getClassLoader().getResourceAsStream("assets/markup.coffee"))))
    val unwrap:Function = context.get("__unwrapModule")
    val render:Function = context.get("__renderModule")

    def compile(bundle:Bundle):Module = {
      val context = new Context(this.context)
      val result = new Module()

      var module:Scriptable = null

      for(i <- bundle.modules) {
        val file = new File(i).getCanonicalFile()
        context.evaluateString(coffee.compile(file.getPath(),FileUtils.readFileToString(file)))

        module = context.get("module")
        val unwrapped = withContext(ctx => unwrap.call(ctx,context.scope,module,Array(module)).asInstanceOf[Scriptable])

        result.inlines.addAll(Context.toType(unwrapped.get("inline",unwrapped),classOf[List[String]]))
        result.markup.add(unwrapped.get("markup",unwrapped).asInstanceOf[Scriptable])
      }

      result.source = module

      if(result.source == null) {
        throw new Exception("source module is null for '"+bundle.name+"'")
      }

      result
    }
  }
}