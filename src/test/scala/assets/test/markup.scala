package assets.test

import org.junit._
import org.scalatest.junit._
import assets._

class Markup extends AssertionsForJUnit {
  val coffee = new Coffee(new Module())

  val module = new Module()
  module.evaluateString(coffee.compile("assets/markup.coffee"))

  val markup = new assets.Markup(module,"modules/main.coffee")

  @Test
  def initialize() {
    assert(markup.includes.size() === 1)
    assert(markup.styles.size() === 2)
    assert(markup.scripts.size() === 2)
  }

  @Test
  def render() {
    System.out.println("render: "+markup.render())
  }
}