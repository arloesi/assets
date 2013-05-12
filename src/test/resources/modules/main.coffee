
this.module =
  include:  ["common"]

  scripts:  ["common","main"]
  styles:   ["common","main"]

  markup: master
    head: ->
      title "Home"

    body: ->
      div "Content"

