package assets

import org.lesscss.LessCompiler

class Less {
  LessCompiler compiler = new LessCompiler()

  Less() {
  }

  String compile(String source) {
    return compiler.compile(source);
  }
}