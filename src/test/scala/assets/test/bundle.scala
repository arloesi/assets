package assets.test

import java.io._
import java.util._

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

import org.junit._
import org.scalatest.junit._
import assets._

import groovy.util.ConfigSlurper

class Bundle extends AssertionsForJUnit {
  type Map = java.util.Map[String,Object]

  val markup = new assets.Bundle("bundle") {
    override val scripts:List[String] = "assets/scripts/common.coffee"::"assets/scripts/main.coffee"::Nil
    override val styles:List[String] = "assets/styles/common.less"::"assets/styles/main.less"::Nil
  }

  @Test
  def render() {
    System.out.println("script: "+markup.script)
    System.out.println("styles: "+markup.style)
  }

  @Test
  def config() {
    val config = new ConfigSlurper().parse(getClass().getClassLoader().getResource("assets.gradle"))
    val bundles = config.getProperty("bundles").asInstanceOf[Map]
    val modules = config.getProperty("modules").asInstanceOf[Map]
    assert(bundles.size() === 1)
    assert(modules.size() === 2)
  }
}