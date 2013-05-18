
menu =
  script: ->
    class this.Utility
      constructor: (i) ->
        this.items = [{title:"Home",target:"home"},{title:"Content",target:"content"}]

  template: ->
    div "Utility"

this.module =
  inline: [menu.script]

  markup:
    menu: menu.template
