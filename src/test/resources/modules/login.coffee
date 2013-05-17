
this.module =
  inline: []
  markup: []

  master:
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

