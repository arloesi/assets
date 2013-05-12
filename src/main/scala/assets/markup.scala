package assets

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.mozilla.javascript._

object Markup {
  type Config = {
    val stylePaths:List[String]
    val scriptPaths:List[String]
    val includePaths:List[String]
    val outputPath:String
  }
}

class Markup(root:Module,config:Markup.Config,val name:String,coffee:Coffee,less:Less) {
  import Markup._
  type Compiler = {val extension:String;def compile(s:String,x:String):String}

  val module = new Module(root)
  val includes = new LinkedHashSet[String]()
  val inlines = new LinkedList[String]()
  val scripts = new LinkedHashMap[String,String]()
  val styles = new LinkedHashMap[String,String]()
  val scope = load(name)

  lazy val markup = {
    val render = scope.get("render",scope).asInstanceOf[Function]
    val ctx = Context.enter()

    val markup = render.call(ctx,module.scope,module.scope,Array(
      config.outputPath+"/styles/"+name+".css?version="+DigestUtils.md5Hex(style),
      config.outputPath+"/scripts/"+name+".js?version="+DigestUtils.md5Hex(script)))

    Context.exit()

    markup
  }

  lazy val script = {
    val builder = new StringBuilder()
    scripts.values().foreach(builder.append _)
    inlines.foreach(builder.append _)
    builder.toString()
  }

  lazy val style = {
    val builder = new StringBuilder()
    styles.values().foreach(builder.append _)
    builder.toString()
  }

  lazy val inputs = {
    val inputs = new LinkedList[String]()
    inputs.addAll(includes)
    inputs.addAll(scripts.keySet())
    inputs.addAll(styles.keySet())
    inputs
  }

  lazy val outputs = {
    val outputs = new LinkedList[String]()
    outputs.add(config.outputPath+"/modules/"+name+".html")
    outputs.add(config.outputPath+"/scripts/"+name+".js")
    outputs.add(config.outputPath+"/styles/"+name+".css")
    outputs
  }

  def load(name:String):Scriptable = {
    var source:String = null
    var stream:InputStream = null

    breakable {
      for(i <- config.includePaths) {
        val path = i+"/"+name+".coffee"
        stream = getClass().getClassLoader().getResourceAsStream(path)

        if(stream != null) {
          source = path
          break
        }
      }
    }

    module.evaluateReader(source,new StringReader(coffee.compile(name,IOUtils.toString(stream))))

    val ctx = Context.enter()
    val unwrap:Function = root.get("__markup_unwrap_module")
    val init:Scriptable = module.get("module")
    val scope = unwrap.call(ctx,module.scope,module.scope,Array(init)).asInstanceOf[Scriptable]
    Context.exit()

    for(i <- Context.toType(scope.get("include",scope),classOf[List[String]]).asInstanceOf[List[String]]) {
      if(!includes.contains(i)) {
        includes.add(i)
        load(i)
      }
    }

    inlines.addAll(Context.toType(scope.get("inlines",scope),classOf[List[String]]).asInstanceOf[List[String]])

    for(script <- Context.toType(scope.get("scripts",scope),classOf[List[String]]).asInstanceOf[List[String]]) {
      compile(script,scripts,config.scriptPaths,
        new {val extension = "js";def compile(s:String,x:String) = x}::
        new {val extension = "coffee";def compile(s:String,x:String) = coffee.compile(s,x)}::Nil)
    }

    for(style <- Context.toType(scope.get("styles",scope),classOf[List[String]]).asInstanceOf[List[String]]) {
      compile(style,styles,config.stylePaths,
        new {val extension = "css";def compile(s:String,x:String) = x}::
        new {val extension = "less";def compile(s:String,x:String) = less.compile(x)}::Nil)
    }

    scope
  }

  def compile(source:List[String],paths:List[String],compilers:List[Compiler]):LinkedHashMap[String,String] = {
    val included = new LinkedHashMap[String,String]()
    source.foreach(i => compile(i,included,paths,compilers))
    included
  }

  def compile(source:String,included:LinkedHashMap[String,String],paths:List[String],compilers:List[Compiler]) {
    breakable {
      for(path <- paths) {
        for(compiler <- compilers) {
          val file = path+"/"+source+"."+compiler.extension
          val stream = getClass().getClassLoader().getResourceAsStream(file)

          if(stream != null) {
            if(included.get(file) == null) {
              included.put(file, compiler.compile(file,IOUtils.toString(stream)))
            }

            break
          }
        }
      }
    }
  }
}