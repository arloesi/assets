package assets

import java.io._
import java.util.{List,LinkedList,LinkedHashMap,HashMap,HashSet,Arrays,Collections}
import java.util.zip._
import java.security._;
import scala.collection.JavaConversions._
import groovy.util.ConfigSlurper

import org.apache.commons.io._
import org.apache.commons.codec.digest.DigestUtils

import org.gradle.api.{Project,DefaultTask,Task}
import org.gradle.api.plugins._
import org.gradle.api.tasks._

import org.cyberneko.html._
import org.cyberneko.html.filters._
import org.apache.xerces.xni.parser.XMLInputSource

import com.google.javascript.jscomp._

object Assets {
  import Compiler._

  val start = System.currentTimeMillis()
  def seconds() = {(System.currentTimeMillis()-start)*0.001}

  println("init: "+seconds())

  final val TEMP = "assets-temp/"

  println("context: "+seconds())
  val context = new Context()
  println("coffee: "+seconds())
  val coffee = new Coffee(context)
  println("less: "+seconds())
  val less = new Less()
  println("markup: "+seconds())
  val markup = new Markup(context,coffee)
  println("starting: "+seconds())

  def md5(source:String) = {
      val md = MessageDigest.getInstance("MD5");
      val dataBytes = source.getBytes()
      md.update(dataBytes, 0, dataBytes.length);
      val mdbytes = md.digest();

      val sb = new StringBuffer();
      for (i <- 0 until mdbytes.length) {
        sb.append(Integer.toString((mdbytes(i) & 0xff) + 0x100, 16).substring(1));
      }

      sb.toString()
  }

  def sourcePath(source:String) = {
    new File(source).getCanonicalPath()
  }

  def targetPath(project:Project,target:String) = {
    new File(project.getBuildDir().getPath()+"/"+target).getCanonicalPath()
  }

  def scriptPath(script:String) = {
    TEMP+"/scripts/"+FilenameUtils.getPathNoEndSeparator(script)+"/"+FilenameUtils.getBaseName(script)+".js"
  }

  def stylePath(script:String) = {
    TEMP+"/styles/"+FilenameUtils.getPathNoEndSeparator(script)+"/"+FilenameUtils.getBaseName(script)+".css"
  }

  def imagePath(image:String) = {
    "assets/"+image
  }

  def compression(optimize:Boolean) = {
    if(optimize) {
      Deflater.BEST_COMPRESSION
    } else {
      Deflater.BEST_SPEED
    }
  }

  def compress(file:File,source:String,optimize:Boolean) {
    val stream = new GZIPOutputStream(new FileOutputStream(file),compression(optimize))
    IOUtils.write(source,stream)
    stream.close()
  }

  def compress(file:File,source:File,optimize:Boolean) {
    val stream = new GZIPOutputStream(new FileOutputStream(file),compression(optimize))
    IOUtils.copy(new FileInputStream(source), stream)
    stream.close()
  }

  def formatHtml(html:String,optimize:Boolean) = {
    if(false) {
      /*val writer = new java.io.StringWriter()
      val filter = new org.cyberneko.html.filters.Writer(writer,"UTF-8")
      val purifier = new org.cyberneko.html.filters.Purifier()
      val filters = Array(purifier,filter)
      val parser = new HTMLConfiguration();
      parser.setProperty("http://cyberneko.org/html/properties/filters",filters);
      val input = new XMLInputSource(null,null,null,new java.io.StringReader(html),null)
      parser.parse(input)
      writer.toString()*/
      html
    } else {
      html
    }
  }

  def formatJS(script:String,optimize:Boolean):String = {
    if(optimize) {
      val externs = Collections.emptyList();
      val inputs = Arrays.asList(JSSourceFile.fromCode("default.js",script));

      val options = new CompilerOptions();
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

      val compiler = new com.google.javascript.jscomp.Compiler();
      val result = compiler.compile(externs, inputs, options);

      compiler.toSource()
    } else {
      script
    }
  }

  def formatCSS(css:String,optimize:Boolean):String = {
    if(optimize) {
      less.compress(css)
    } else {
      css
    }
  }

  class Extension {
    private var optimize = false
    def setOptimize(value:Boolean) {this.optimize=value}
    def getOptimize() = {this.optimize}
  }

  class AssetsTask extends DefaultTask {
    @TaskAction
    def compile() {
    }
  }

  abstract class FileTask extends DefaultTask {
    lazy val source = getInputs().getFiles().first
    lazy val target = getOutputs().getFiles().first

    @TaskAction
    def compile()

    def load() = {
      FileUtils.readFileToString(source)
    }

    def save(source:String) {
      val optimize = getProject().getExtensions().getByType(classOf[Extension]).getOptimize()
      FileUtils.writeStringToFile(target, source)
      compress(new File(target.getPath()+".gz"),source,optimize)
    }
  }

  class CopyTask extends FileTask {
    var compress:Boolean = true

    override def compile() {
      FileUtils.copyFile(source, target)

      if(compress) {
        Assets.compress(
          new File(target.getPath()+".gz"),source,
          getProject().getExtensions().getByType(classOf[Extension]).getOptimize())
      }
    }
  }

  class CoffeeTask extends FileTask {
    override def compile() {
      save(coffee.compile(source.getPath(),load()))
    }
  }

  class LessTask extends FileTask {
    override def compile() {
      save(less.compile(source.getPath(),load()))
    }
  }

  class ModuleTask extends DefaultTask {
    var bundle:Bundle = null

    @TaskAction
    def compile() {
      val optimize = getProject().getExtensions().getByType(classOf[Extension]).getOptimize()

      val baseDir = getProject().getBuildDir()+"/assets"
      val module = markup.compile(bundle)
      val script = new StringBuilder()

      bundle.scripts_r(i => script.append(FileUtils.readFileToString(
        new File(targetPath(getProject(),scriptPath(i._2))))))

      module.inlines.foreach(script.append _)

      val style = new StringBuilder()
      bundle.styles_r(i => style.append(FileUtils.readFileToString(
        new File(targetPath(getProject(),stylePath(i._2))))))

      val js = formatJS(script.toString(),optimize)
      val css = formatCSS(style.toString(),optimize)

      val scriptName = md5(js).toString()
      val scriptFile = new File(baseDir+"/"+bundle.name+".js")
      FileUtils.writeStringToFile(scriptFile, js)
      compress(new File(scriptFile.getAbsolutePath()+".gz"),js,optimize)

      val styleName = md5(css).toString()
      val styleFile = new File(baseDir+"/"+bundle.name+".css")
      FileUtils.writeStringToFile(styleFile, css)
      compress(new File(styleFile.getAbsolutePath()+".gz"),css,optimize)

      val html = formatHtml(module.master(bundle,scriptName,styleName),optimize)
      val htmlFile = new File(getProject().getBuildDir()+"/modules/"+bundle.name+".html")
      FileUtils.writeStringToFile(htmlFile,html)
      compress(new File(htmlFile.getAbsolutePath()+".gz"),html,optimize)
    }
  }
}

class Assets extends org.gradle.api.Plugin[Project] {
  import Assets._

  def apply(project:Project) {
    println("start: "+seconds())
    project.getExtensions().create("assets",classOf[Extension])

    val factory = new HashMap[String,Bundle]()
    val bundles = new LinkedList[Bundle]()
    val imports = new HashSet[String]()
    val main = project.task(Map("type"->classOf[AssetsTask]),"assets")

    def parse(source:String,root:Boolean) {
      println("parse: "+source+" => "+seconds())
      val config = new ConfigSlurper().parse(FileUtils.readFileToString(new File(source+"/assets.gradle")))

      config.getProperty("imports") match {
        case list:List[String] => list.foreach(imports.add _)
        case _ => ()
      }

      config.getProperty("include") match {
        case list:List[String] => list.foreach(i => parse(i,false))
        case _ => ()
      }

      config.getProperty("bundles") match {
        case null => ()
        case modules:java.util.Map[String,Object] => {
          for((k,v) <- modules) {
            val bundle = new Bundle(factory,k,source,v.asInstanceOf[java.util.Map[String,Object]]) {
              override def initialize() {
                println("images: "+seconds())
                buildImageTasks(project,images)
                println("scripts: "+seconds())
                buildScriptTasks(project,scripts)
                println("styles: "+seconds())
                buildStyleTasks(project,styles)
              }
            }

            factory.put(bundle.name, bundle)
            bundles.add(bundle)
          }
        }
      }

      config.getProperty("modules") match {
        case null => ()
        case modules:java.util.Map[String,Object] => {
          for((k,v) <- modules) {
            if(root || imports.contains(k)) {
              val bundle = new Bundle(factory,k,source,v.asInstanceOf[java.util.Map[String,Object]]) {
                override def initialize() {
                  println("images: "+seconds())
                  buildImageTasks(project,images)
                  println("scripts: "+seconds())
                  buildScriptTasks(project,scripts)
                  println("styles: "+seconds())
                  buildStyleTasks(project,styles)
                  println("module: "+seconds())
                  buildModuleTask(project,this)

                  main.dependsOn(name)
                  images_r(i => main.dependsOn(targetPath(project,imagePath(i._2))))
                }
              }

              bundles.add(bundle)
            }
          }
        }
      }
    }

    parse(".",true)
    println("initialize: "+seconds())
    bundles.foreach(i => i.initialize())
    println("finish: "+seconds())
  }

  def buildImageTasks(project:Project,images:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((s,t) <- images) {
      val task = buildCopyTask(project,sourcePath(s),imagePath(t),false)
      tasks.add(task)
    }

    tasks
  }

  def buildScriptTasks(project:Project,scripts:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((source,targ) <- scripts) {
      val target = scriptPath(targ)
      val ext = FilenameUtils.getExtension(source)

      if(ext == "js") {
        tasks.add(buildCopyTask(project,source,target))
      } else {
        tasks.add(buildCoffeeTask(project,source,target))
      }
    }

    tasks
  }

  def buildStyleTasks(project:Project,styles:List[(String,String)]) = {
    val tasks = new LinkedList[Task]()

    for((source,targ) <- styles) {
      val target = stylePath(targ)
      val ext = FilenameUtils.getExtension(source)

      if(ext == "css") {
        tasks.add(buildCopyTask(project,source,target))
      } else {
        tasks.add(buildLessTask(project,source,target))
      }
    }

    tasks
  }

  def buildModuleTask(project:Project,bundle:Bundle) {
    val task = project.task(Map("type"->classOf[ModuleTask]),bundle.name).asInstanceOf[ModuleTask]
    task.bundle = bundle

    bundle.scripts_r(x => {
      val path = targetPath(project,scriptPath(x._2))
      task.dependsOn(path)
      task.getInputs().file(path)
    })

    bundle.styles_r(x => {
      val path = targetPath(project,stylePath(x._2))
      task.dependsOn(path)
      task.getInputs().file(path)
    })

    bundle.modules.foreach(task.getInputs().file _)

    val script = targetPath(project,"assets/"+bundle.name+".js")
    task.getOutputs().file(script)
    task.getOutputs().file(script+".gz")

    val style = targetPath(project,"assets/"+bundle.name+".css")
    task.getOutputs().file(style)
    task.getOutputs().file(style+".gz")

    val html = targetPath(project,"modules/"+bundle.name+".html")
    task.getOutputs().file(html)
    task.getOutputs().file(html+".gz")

    task
  }

  def buildFileTask[T](project:Project,`type`:Class[T],source:String,target:String,compress:Boolean=true) = {
    val task = project.task(Map("type"->`type`),targetPath(project,target))
    val input = new File(source).getCanonicalPath()
    val output = targetPath(project,target)
    task.getInputs().file(input)
    task.getOutputs().file(output)

    if(compress) {
      task.getOutputs().file(output+".gz")
    }

    task.asInstanceOf[T]
  }

  def buildCopyTask(project:Project,source:String,target:String,compress:Boolean=true) = {
    val task = buildFileTask(project,classOf[CopyTask],source,target,compress)
    task.compress = compress
    task
  }

  def buildCoffeeTask(project:Project,source:String,target:String) = {
    buildFileTask(project,classOf[CoffeeTask],source,target)
  }

  def buildLessTask(project:Project,source:String,target:String) = {
    buildFileTask(project,classOf[LessTask],source,target)
  }
}
