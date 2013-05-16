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

class Bundle(val factory:HashMap[String,Bundle],val name:String,val assets:Map[String,Object]) {
  import Bundle._

  type Node = Map[String,Object]
  val source = new LinkedList[String]()

  lazy val scripts =
    assets.get("styles") match {
      case null => new LinkedList[String]()
      case list:List[String] => list
    }

  lazy val styles =
    assets.get("styles") match {
      case null => new LinkedList[String]()
      case list:List[String] => list
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

        parse("",images)

        list
      }
    }

  def parse_r(f:Bundle=>Unit) {
    def parse(bundle:Bundle) {
      for(i <- bundle.assets.get("includes").asInstanceOf[List[String]]) {
        parse(factory.get(i))
      }

      f(bundle)
    }

    parse(this)
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