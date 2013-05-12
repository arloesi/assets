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
    val includePaths:List[String]
  }
}

class Markup(module:Module,config:Markup.Config,source:String,coffee:Coffee,less:Less) {
  import Markup._
  type Compiler = {val extension:String;def compile(s:String,x:String):String}

  val scope = load(source)
  val includes = new LinkedHashSet[String]()
  val inlines = new LinkedList[String]()
  val scripts = new LinkedHashMap[String,String]()
  val styles = new LinkedHashMap[String,String]()
  val templates = new LinkedList[String]()

  def load(name:String):Scriptable = {
    var source:String = null

    breakable {
      for(i <- config.includePaths) {
        val file = new File(i+"/"+name+".coffee")

        if(file.exists()) {
          source = file.getPath()
          break
        }
      }
    }

    module.evaluateFile(source)

    val ctx = Context.enter()
    val unwrap:Function = module.get("__markup_unwrap_module")
    val scope = unwrap.call(ctx,module.scope,module.scope,Array()).asInstanceOf[Scriptable]
    Context.exit()

    for(i <- scope.get("include",scope).asInstanceOf[List[String]]) {
      if(!includes.contains(i)) {
        includes.add(i)
        load(i)
      }
    }

    inlines.addAll(scope.get("inlines",scope).asInstanceOf[List[String]])
    templates.addAll(scope.get("templates",scope).asInstanceOf[List[String]])

    for(script <- scope.get("scripts",scope).asInstanceOf[List[String]]) {
      compile(script,scripts,config.scriptPaths,
        new {val extension = "js";def compile(s:String,x:String) = x}::
        new {val extension = "coffee";def compile(s:String,x:String) = coffee.compile(s,x)}::Nil)
    }

    for(style <- scope.get("styles",scope).asInstanceOf[List[String]]) {
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
          val file = new File(path+"/"+source+"."+compiler.extension)

          if(file.exists()) {
            if(included.get(file.getPath()) == null) {
              included.put(file.getPath(), compiler.compile(file.getPath(),FileUtils.readFileToString(file)))
            }

            break
          }
        }
      }
    }
  }
}