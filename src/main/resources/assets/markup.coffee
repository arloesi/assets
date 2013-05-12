# **CoffeeKup** lets you to write HTML templates in 100% pure
# [CoffeeScript](http://coffeescript.org).
#
# You can run it on [node.js](http://nodejs.org) or the browser, or compile your
# templates down to self-contained javascript functions, that will take in data
# and options and return generated HTML on any JS runtime.
#
# The concept is directly stolen from the amazing
# [Markaby](http://markaby.rubyforge.org/) by Tim Fletcher and why the lucky
# stiff.

this.java =  Packages.java

markup = this
version = '0.1'
# coffee = CoffeeScript

# Values available to the `doctype` function inside a template.
# Ex.: `doctype 'strict'`
doctypes =
  'default': '<!DOCTYPE html>'
  '5': '<!DOCTYPE html>'
  'xml': '<?xml version="1.0" encoding="utf-8" ?>'
  'transitional': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">'
  'strict': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">'
  'frameset': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">'
  '1.1': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">',
  'basic': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML Basic 1.1//EN" "http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd">'
  'mobile': '<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.2//EN" "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd">'
  'ce': '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "ce-html-1.0-transitional.dtd">'

# CoffeeScript-generated JavaScript may contain anyone of these; but when we
# take a function to string form to manipulate it, and then recreate it through
# the `Function()` constructor, it loses access to its parent scope and
# consequently to any helpers it might need. So we need to reintroduce these
# inside any "rewritten" function.
coffeescript_helpers = """
  var __slice = Array.prototype.slice;
  var __hasProp = Object.prototype.hasOwnProperty;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  var __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype;
    return child; };
  var __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    } return -1; };
""".replace /\n/g, ''

# Private HTML element reference.
# Please mind the gap (1 space at the beginning of each subsequent line).
elements =
  # Valid HTML 5 elements requiring a closing tag.
  # Note: the `var` element is out for obvious reasons, please use `tag 'var'`.
  regular: 'a abbr address article aside audio b bdi bdo blockquote body button
 canvas caption cite code colgroup datalist dd del details dfn div dl dt em
 fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6 head header hgroup
 html i iframe ins kbd label legend li map mark menu meter nav noscript object
 ol optgroup option output p pre progress q rp rt ruby s samp script section
 select small span strong style sub summary sup table tbody td textarea tfoot
 th thead time title tr u ul video'

  # Valid self-closing HTML 5 elements.
  void: 'area base br col command embed hr img input keygen link meta param
 source track wbr'

  obsolete: 'applet acronym bgsound dir frameset noframes isindex listing
 nextid noembed plaintext rb strike xmp big blink center font marquee multicol
 nobr spacer tt'

  obsolete_void: 'basefont frame'

# Create a unique list of element names merging the desired groups.
merge_elements = (args...) ->
  result = []
  for a in args
    for element in elements[a].split ' '
      result.push element unless element in result
  result

# Public/customizable list of possible elements.
# For each name in this list that is also present in the input template code,
# a function with the same name will be added to the compiled template.
tags = merge_elements 'regular', 'obsolete', 'void', 'obsolete_void'

# Public/customizable list of elements that should be rendered self-closed.
self_closing = merge_elements 'void', 'obsolete_void'

# This is the basic material from which compiled templates will be formed.
# It will be manipulated in its string form at the `markup.compile` function
# to generate the final template function.

data = {format:true,autoescape:false}

# Internal CoffeeKup stuff.

text = (txt) ->
  __ck.buffer.push String(txt)
  null

__ck =
  buffer:
    push: (x) -> writer.write(x)

  esc: (txt) -> String(txt)

  tabs: 0

  repeat: (string, count) -> Array(count + 1).join string

  indent: -> text @repeat('  ', @tabs) if data.format

  # Adapter to keep the builtin tag functions DRY.
  tag: (name, args) ->
    combo = [name]
    combo.push i for i in args
    tag.apply data, combo

  render_idclass: (str) ->
    classes = []

    for i in str.split '.'
      if '#' in i
        id = i.replace '#', ''
      else
        classes.push i unless i is ''

    text " id=\"#{id}\"" if id

    if classes.length > 0
      text " class=\""
      for c in classes
        text ' ' unless c is classes[0]
        text c
      text '"'

  render_attrs: (obj, prefix = '') ->
    for k, v of obj
      # `true` is rendered as `selected="selected"`.
      v = k if typeof v is 'boolean' and v

      # Functions are rendered in an executable form.
      v = "(#{v}).call(this);" if typeof v is 'function'

      # Prefixed attribute.
      if typeof v is 'object' and v not instanceof Array
        # `data: {icon: 'foo'}` is rendered as `data-icon="foo"`.
        @render_attrs(v, prefix + k + '-')
      # `undefined`, `false` and `null` result in the attribute not being rendered.
      else if v
        # strings, numbers, arrays and functions are rendered "as is".
        text " #{prefix + k}=\"#{@esc(v)}\""

  render_contents: (contents) ->
    switch typeof contents
      when 'string', 'number', 'boolean'
        text @esc(contents)
      when 'function'
        text '\n' if data.format
        @tabs++
        result = contents.call data
        if typeof result is 'string'
          @indent()
          text @esc(result)
          text '\n' if data.format
        @tabs--
        @indent()

  render_tag: (name, idclass, attrs, contents) ->
    @indent()

    text "<#{name}"
    @render_idclass(idclass) if idclass
    @render_attrs(attrs) if attrs

    if name in self_closing
      text ' />'
      text '\n' if data.format
    else
      text '>'

      @render_contents(contents)

      text "</#{name}>"
      text '\n' if data.format

    null

markup.tag = tag = (name, args...) ->
  for a in args
    switch typeof a
      when 'function'
        contents = a
      when 'object'
        attrs = a
      when 'number', 'boolean'
        contents = a
      when 'string'
        if args.length is 1
          contents = a
        else
          if a is args[0]
            idclass = a
          else
            contents = a

  __ck.render_tag(name, idclass, attrs, contents)

markup.h = (txt) ->
  String(txt).replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

markup.doctype = (type = 'default') ->
  text doctypes[type]
  text '\n' if data.format

markup.text = text

markup.comment = (cmt) ->
  text "<!--#{cmt}-->"
  text '\n' if data.format

markup.coffee = (param) ->
  tag "script", type:"text/javascript", "\n// <![CDATA[\n(#{param}).call(this);\n// ]]>\n"

# Conditional IE comments.
markup.ie = (condition, contents) ->
  __ck.indent()

  text "<!--[if #{condition}]>"
  __ck.render_contents(contents)
  text "<![endif]-->"
  text '\n' if data.format

tag_impl = (i) ->
  (a...) -> tag i, a...

for i in tags
  markup[i] = tag_impl i

markup.runtime = ->
  tag "script",type:"text/javascript",coffeescript_helpers

writer =
  write: (x) ->
    this.buffer.push x

markup.render = (template) ->
  holder = writer.buffer
  writer.buffer = buffer = []
  template()
  writer.buffer = holder
  buffer.join ""

unwrapList = (i) ->
  if typeof i == "undefined"
    new java.util.LinkedList()
  else
    list = new java.util.LinkedList()
    list.add i for i in items
    list

this.__markup_unwrap_module = ->
  includes: unwrapList module.includes
  scripts: unwrapList module.scripts
  styles: unwrapList module.styles

  render: (scripts,styles) ->
    markup.render ->
      html ->
        head ->
          runtime()

          link rel:"stylesheet",href:i for i in styles
          script type:"tet/javascript",src:i for i in scripts

          module.markup.head()

        body ->
          module.markup.body()

