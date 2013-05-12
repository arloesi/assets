
this.module =
  include:  ["common"]

  scripts:  ["common","login"]
  styles:   ["common","login"]

  markup: master
    head: ->
      title "Login"

    body: ->
      div ->
        form ->
          label ->
            text "Name:&nbsp;"
            input type:"text"

          label ->
            text "Pass:&nbsp;"
            input type:"text"

