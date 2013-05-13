package assets.test

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.junit._
import org.scalatest.junit._
import assets._

class Bundle extends AssertionsForJUnit {
  val markup = new assets.Bundle("bundle") {
    override val scripts:List[String] = "assets/scripts/common.coffee"::"assets/scripts/main.coffee"::Nil
    override val styles:List[String] = "assets/styles/common.less"::"assets/styles/main.less"::Nil
  }

  @Test
  def render() {
    // System.out.println("markup: "+markup.markup)
    System.out.println("script: "+markup.script)
    System.out.println("styles: "+markup.style)
  }
}