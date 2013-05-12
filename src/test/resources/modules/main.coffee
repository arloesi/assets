
this.module =
  include:  ["common"]

  scripts:  ["common","main"]
  styles:   ["common","main"]

  markup: master
    head: ->
      title " My Title"

    body: ->
      div "My Content"

