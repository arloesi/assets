package assets.test

import org.junit.*
import org.mozilla.javascript.*
import assets.*

class Assets {
  @Test
  void script() {
    def module = new Module()
    module.evaluateString("this.count=4")
    def count = module.scope.get("count")
    Assert.assertEquals(4.0,count,0.0)
  }

  @Test
  void coffee() {
    def coffee = new Coffee(new Module())
    def script = coffee.compile("<inline>","x=4\nthis.y=5")

    def module = new Module()
    module.evaluateString(script)

    Assert.assertEquals(null, module.scope.get("x"))
    Assert.assertEquals(5.0, module.scope.get("y"),0.0)
  }

  @Test
  void less() {
    def less = new Less()
    def style = less.compile("@padding:8px;\n.dialog {padding:@padding;}\n")
    System.out.println("style:"+style)
  }
}