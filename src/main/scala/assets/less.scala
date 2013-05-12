package assets

import org.lesscss.LessCompiler

class Less {
  val compiler = new LessCompiler()

  def compile(source:String) = {
    compiler.compile(source);
  }
}