
this.module =
  include:  ["common","utilities"]

  scripts:  ["common","main"]
  styles:   ["common","main"]

  markup: master
    head: ->
      title " My Title"

    body: ->
      div "My Content"

