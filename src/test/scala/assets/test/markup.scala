package assets.test

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.junit._
import org.scalatest.junit._
import assets._

class Markup extends AssertionsForJUnit {
  val coffee = new Coffee(new Module())
  val less = new Less()

  val module = new Module()
  module.evaluateString(coffee.compile("assets/markup.coffee"))

  val markup = new assets.Markup(module,
    new {
      val stylePaths:List[String] = "assets/styles"::Nil;
      val scriptPaths:List[String]  = "assets/scripts"::Nil;
      val includePaths:List[String]  = "modules"::Nil;
      val outputPath:String = "assets"},
    "main",coffee,less)

  @Test
  def initialize() {
    /*assert(markup.includes.size() === 1)
    assert(markup.styles.size() === 2)
    assert(markup.scripts.size() === 2)*/
  }

  @Test
  def render() {
    System.out.println("render: "+markup.markup)
  }
}