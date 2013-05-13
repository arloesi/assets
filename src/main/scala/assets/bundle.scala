package assets

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.mozilla.javascript._

object Bundle {
  val context = new Context()
  val compilers = new HashMap[String,Compiler]()

  def loadMarkup(scope:Object):List[String] = {
    null
  }
}

abstract class Bundle(val name:String) {
  import Bundle._

  val context = new Context(Bundle.context)
  val loader = getClass().getClassLoader()

  val bundles:List[Bundle] = Nil
  val scripts:List[String] = Nil
  val styles:List[String] = Nil

  lazy val module = {
    context.evaluateReader(name, new StringReader(
      compilers.get("coffee").compile(name,
        IOUtils.toString(loader.getResourceAsStream("modules/"+name+".coffee")))))
    context.get("module").asInstanceOf[Scriptable]
  }

  lazy val script:String = {
    val builder = new StringBuilder()

    for(i <- bundles) {
      builder.append(i.script)
    }

    for(i <- scripts) {
      builder.append(IOUtils.toString(loader.getResourceAsStream(i)))
    }

    builder.toString()
  }

  lazy val style:String = {
    val builder = new StringBuilder()

    for(i <- bundles) {
      builder.append(i.style)
    }

    for(i <- styles) {
      builder.append(IOUtils.toString(loader.getResourceAsStream(i)))
    }

    builder.toString()
  }

  lazy val markup = loadMarkup(module.get("markup",module))
}

abstract class Module(name:String,compilers:Map[String,Compiler]) extends Bundle(name) {

}