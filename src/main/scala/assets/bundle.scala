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

class Bundle(val name:String,assets:Map[String,Object]) {
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
}