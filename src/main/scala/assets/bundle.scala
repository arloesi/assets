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
      case null => new LinkedList[(String,String)]()
      case list:List[String] => list.map(i => (source+"/"+i,this.name+"/"+i)):List[(String,String)]
    }

  lazy val styles =
    assets.get("styles") match {
      case null => new LinkedList[String]()
      case list:List[String] => list.map(i => source+"/"+i):List[String]
    }

  lazy val images =
    assets.get("images") match {
      case null => new LinkedList[(String,String)]()
      case images:Object => {
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
                val file = new File(this.source+"/"+i)

                if(file.isFile()) {
                  val relative = FilenameUtils.getName(file.getName())
                  list.add((file.getCanonicalPath(),target+"/"+relative))
                } else if(file.isDirectory()) {
                  val path = file.getCanonicalPath()

                  for(r <- matcher.getResources("file:"+file.getCanonicalPath()+"/**/*.png")) {
                    val relative = r.getFile().getCanonicalPath().substring(path.length())
                    list.add((r.getFile().getCanonicalPath(),target+"/"+relative))
                  }
                } else {
                  throw new FileNotFoundException(file.getPath())
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
      bundle.assets.get("include") match {
        case list:List[String] =>
          for(i <- list) {
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

  def scripts_r(f:((String,String))=>Unit) {
    parse_r(x => x.scripts.foreach(f))
  }

  def styles_r(f:String=>Unit) {
    parse_r(x => x.styles.foreach(f))
  }

  def images_r(f:((String,String))=>Unit) {
    parse_r(x => x.images.foreach(f))
  }
}