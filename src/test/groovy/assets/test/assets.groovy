package assets.test

import org.junit.*
import org.mozilla.javascript.*
import assets.*

class Assets {
  @Test
  void module() {
    def module = new Module()
    module.evaluateString("this.count=4")
    def count = module.scope.get("count")
    Assert.assertEquals 4,count
  }

  @Test
  void coffee() {

  }

  @Test
  void less() {

  }
}