package assets

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.mozilla.javascript._

import org.springframework.core.io.support.PathMatchingResourcePatternResolver

object Bundle {
  val matcher = new PathMatchingResourcePatternResolver()
  val coffee = new Coffee(new Context())
  val less = new Less()

  val context = new Context()
  context.evaluateReader("markup",
    new StringReader(coffee.compile("markup",
      IOUtils.toString(getClass().getClassLoader().getResourceAsStream("assets/markup.coffee")))))

  val compilers = new HashMap[String,Compiler]()

  def loadMarkup(scope:Object):List[String] = {
    null
  }

  def loadInline(scope:Object):List[String] = {
    null
  }

  def loadImages(assets:Map[String,Object]) = {
    val images = assets.get("images")
    val list = new LinkedList[(String,String)]()

    def parse(target:String,source:Object) {
      source match {
        case m:Map[String,Object] => {
          for((k,v) <- m) {
            parse(target+"/"+k,v)
          }
        }
        case l:List[String] => {
          for(i <- l) {
            val f = new File(i)

            if(f.isFile()) {
              list.add((i,target+"/"+i))
            } else if(f.isDirectory()) {
              for(r <- matcher.getResources(i+"/**/*.png")) {
                list.add((r.getFile().getPath(),target+"/"+r.getFile().getPath()))
              }
            } else {
              throw new FileNotFoundException(f.getPath())
            }
          }
        }
      }
    }

    if(images != null) {
      parse("",images)
    }

    list
  }
}

abstract class Bundle(val name:String, assets:Map[String,Object]) {
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
  lazy val inline = loadInline(module.get("inline",module))
}

abstract class Module(name:String,assets:Map[String,Object]) extends Bundle(name,assets) {

}