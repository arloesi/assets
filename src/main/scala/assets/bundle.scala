package assets

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.mozilla.javascript._

// import org.springframework.core.io.support.PathMatchingResourcePatternResolver

object Bundle {
  // val matcher = new PathMatchingResourcePatternResolver()
}

abstract class Bundle(val factory:HashMap[String,Bundle],val name:String, val source:String, val assets:Map[String,Object]) {
  import Bundle._

  type Node = Map[String,Object]

  def initialize()

  lazy val modules = {
    val list = new LinkedList[String]()
    parse_r(x => {
      def path(name:String) = {x.source+"/modules/"+name+".coffee"}

      x.assets.get("modules") match {
        case null => {
          val file = new File(path(x.name)).getCanonicalFile()

          if(file.exists()) {
            list.add(file.getPath())
          }
        }
        case modules:List[String] => {
          for(i <- modules) {
            list.add(new File(path(i)).getCanonicalPath())
          }
        }
      }

    })
    list
  }

  lazy val scripts =
    assets.get("scripts") match {
      case null => new LinkedList[(String,String)]()
      case list:List[String] => list.map(i => (source+"/"+i,this.name+"/"+i)):List[(String,String)]
    }

  lazy val styles =
    assets.get("styles") match {
      case null => new LinkedList[(String,String)]()
      case list:List[String] => list.map(i => (source+"/"+i,this.name+"/"+i)):List[(String,String)]
    }

  def listFiles(list:List[File],file:File) {
    for(i <- file.listFiles()) {
      if(i.isFile()) {
        list.add(i)
      } else {
        listFiles(list,i)
      }
    }
  }

  lazy val images = {
    val list = new LinkedList[(String,String)]()

    for((k,v) <- assets) {
      k match {
        case "scripts" => ()
        case "styles" => ()
        case "modules" => ()
        case "include" => ()
        case name:String => {
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
                    val files = new LinkedList[File]()
                    listFiles(files,file.getCanonicalFile())

                    for(r <- files) {
                      val relative = r.getCanonicalPath().substring(path.length())
                      list.add((r.getCanonicalPath(),target+"/"+relative))
                    }
                  } else {
                    throw new FileNotFoundException(file.getPath())
                  }
                }
              }
            }
          }

          parse(name,v)
        }
      }
    }

    list
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

  def styles_r(f:((String,String))=>Unit) {
    parse_r(x => x.styles.foreach(f))
  }

  def images_r(f:((String,String))=>Unit) {
    parse_r(x => x.images.foreach(f))
  }
}