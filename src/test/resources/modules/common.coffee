
inline ->
  class this.Menu
    constructor: (i) ->
      this.items = [{title:"Home",target:"home"},{title:"Content",target:"content"}]

menu = ->
  div "menu", ->
    div "item", "My Item"
