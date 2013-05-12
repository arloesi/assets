package assets

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.apache.commons.io._

import org.mozilla.javascript._

object Markup {
  type Config = {
    val stylePaths:List[String]
    val scriptPaths:List[String]
  }

  def string(x:String) = x
}

class Markup(module:Module,config:Markup.Config,source:String,coffee:Coffee,less:Less) {
  import Markup._
  type Compiler = {val extension:String;def compile(s:String,x:String):String}

  module.evaluateFile(source)

  val scope = {
    val ctx = Context.enter()
    val unwrap:Function = module.get("__markup_unwrap_module")
    val scope = unwrap.call(ctx,module.scope,module.scope,Array()).asInstanceOf[Scriptable]
    Context.exit()
    scope
  }

  val inputs = new LinkedList[File]()
  val outputs = new LinkedList[File]()

  val includes = new LinkedList[File]()
  val scripts = compile(
    scope.get("scripts",scope).asInstanceOf[List[String]],config.scriptPaths,
    new {val extension = "js";def compile(s:String,x:String) = x}::
    new {val extension = "coffee";def compile(s:String,x:String) = coffee.compile(s,x)}::Nil)

  val inlines = scope.get("inlines",scope).asInstanceOf[List[String]]

  val styles = compile(
    scope.get("styles",scope).asInstanceOf[List[String]],
    config.stylePaths,
      new {val extension = "css";def compile(s:String,x:String) = x}::
      new {val extension = "less";def compile(s:String,x:String) = less.compile(x)}::Nil)

  def render() = {
    ""
  }

  def compile(source:List[String],paths:List[String],compilers:List[Compiler]):LinkedHashMap[String,String] = {
    val included = new LinkedHashMap[String,String]()
    source.foreach(i => compile(i,included,paths,compilers))
    included
  }

  def compile(source:String,included:LinkedHashMap[String,String],paths:List[String],compilers:List[Compiler]) {
    breakable {
      for(path <- paths) {
        for((extension,compile) <- compilers) {
          val file = new File(path+"/"+source+"."+extension)

          if(file.exists()) {
            if(included.get(file.getPath()) == null) {
              included.put(file.getPath(), compile(file.getPath(),FileUtils.readFileToString(file)))
            }

            break
          }
        }
      }
    }
  }
}