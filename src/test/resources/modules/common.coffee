
menu =
  script: ->
    class this.Menu
      constructor: (i) ->
        this.items = [{title:"Home",target:"home"},{title:"Content",target:"content"}]

  markup: ->
    div "menu","data-bind":"with:new Menu()", ->
      div "item", "My Item"

this.module =
  inline: [menu.script]

  markup:
    menu: menu.markup
