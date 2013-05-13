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
}

abstract class Bundle(val name:String) {
  import Bundle._

  val context = new Context(Bundle.context)
  val loader = getClass().getClassLoader()

  val includes:List[Bundle] = Nil
  val scripts:List[String] = Nil
  val styles:List[String] = Nil

  lazy val module = {
    context.evaluateReader(name, new StringReader(
      compilers.get("coffee").compile(name,
        IOUtils.toString(loader.getResourceAsStream("modules/"+name+".coffee")))))
    context.get("module").asInstanceOf[Scriptable]
  }
}

abstract class Module(name:String,compilers:Map[String,Compiler]) extends Bundle(name) {

}