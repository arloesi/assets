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
}

abstract class Bundle(val factory:HashMap[String,Bundle],val name:String, val source:String, val assets:Map[String,Object]) {
  import Bundle._

  type Node = Map[String,Object]

  lazy val includes = {
    val list = new LinkedList[String]()
    parse_r(x => list.add(x.source+"/modules/"+x.name+".coffee"))
    list
  }

  def initialize()

  lazy val scripts =
    assets.get("scripts") match {
      case null => new LinkedList[String]()
      case list:List[String] => list.map(i => source+"/"+i):List[String]
    }

  lazy val styles =
    assets.get("styles") match {
      case null => new LinkedList[String]()
      case list:List[String] => list.map(i => source+"/"+i):List[String]
    }

  lazy val images =
    assets.get("images") match {
      case null => new LinkedList[(String,String)]()
      case images:Node => {
        val list = new LinkedList[(String,String)]()

        def parse(target:String,source:Object) {
          source match {
            case m:Node => {
              for((k,v) <- m) {
                parse(target+"/"+k,v)
              }
            }
            case l:List[String] => {
              for(i <- l) {
                val f = new File(this.source+"/"+i)

                if(f.isFile()) {
                  list.add((i,target+"/"+i))
                } else if(f.isDirectory()) {
                  val path = new File(this.source).getCanonicalPath()

                  for(r <- matcher.getResources("file:"+i+"/**/*.png")) {
                    val relative = r.getFile().getCanonicalPath().substring(path.length())
                    list.add((relative,target+"/"+relative))
                  }
                } else {
                  throw new FileNotFoundException(f.getPath())
                }
              }
            }
          }
        }

        parse(".",images)

        list
      }
    }

  def parse_r(f:Bundle=>Unit) {
    def parse(bundle:Bundle) {
      bundle.assets.get("includes") match {
        case list:List[String] =>
          for(i <- bundle.assets.get("includes").asInstanceOf[List[String]]) {
            parse(factory.get(i))
          }
        case _ => ()
      }

      f(bundle)
    }

    parse(this)
  }

  def bundles_r(f:Bundle=>Unit) {
    parse_r(f)
  }

  def scripts_r(f:String=>Unit) {
    parse_r(x => x.scripts.foreach(f))
  }

  def styles_r(f:String=>Unit) {
    parse_r(x => x.styles.foreach(f))
  }

  def images_r(f:((String,String))=>Unit) {
    parse_r(x => x.images.foreach(f))
  }
}