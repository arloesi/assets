package assets.test

import org.junit._
import org.scalatest.junit._
import org.mozilla.javascript._
import assets._

class Compile extends AssertionsForJUnit {
  type Context = assets.Context

  /*@Test
  def module() {
    val module = new Context()
    module.evaluateString("this.count=4")
    val count = module.get("count")
      .asInstanceOf[Double]
    Assert.assertEquals(4.0,count,0.0)
  }

  @Test
  def coffee() {
    val coffee = new Coffee(new Context())
    val script = coffee.compile("<inline>","x=4\nthis.y=5")
      .asInstanceOf[String]

    val module = new Context()
    module.evaluateString(script)

    Assert.assertNotEquals(4.0, module.get("x"))
    Assert.assertEquals(5.0, module.get("y").asInstanceOf[Double],0.0)
  }

  @Test
  def less() {
    val less = new Less()
    val style = less.compile("@padding:8px;\n.dialog {padding:@padding;}\n")
    Assert.assertEquals(style.replaceAll("\\s+",""), ".dialog{padding:8px;}")
  }*/
}