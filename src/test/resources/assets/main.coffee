
inline ->
  div "xxx..."

this.module =
  scripts: ["common.js","main.js"]
  styles: ["common.css","main.css"]

  markup: master
    head: ->
      title " My Title"

    body: ->
      div "My Content"

