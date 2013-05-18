(function() {
  var Source,
    __slice = [].slice;

  Source = (function() {
    function Source(_arg) {
      var complete, params, query, select;

      select = _arg.select, params = _arg.params, complete = _arg.complete, query = _arg.query;
      this.complete = complete;
      this.attribute = select;
      this.params = params;
      this.entities = {};
      this.collections = {};
      this.query = query;
    }

    Source.prototype.entity = function(id) {
      if (typeof id === "function") {
        id = id();
      }
      if (typeof this.entities[id] === "undefined") {
        this.entities[id] = ko.observable();
      }
      return this.entities[id];
    };

    Source.prototype.collection = function(id) {
      if (typeof id === "function") {
        id = id();
      }
      if (typeof this.collections[id] === "undefined") {
        this.collections[id] = ko.observableArray();
      }
      return this.collections[id];
    };

    Source.prototype.select = function(q) {
      var select, uri;

      if (q == null) {
        q = this.query;
      }
      if (typeof this.attribute === "array") {
        select = this.attribute.map(encodeURIComponent).join();
      } else {
        select = encodeURIComponent(this.attribute);
      }
      uri = "/content/" + select + "?" + (encodeParams(this.params));
      if (typeof q !== "undefined") {
        uri += "&query=" + encodeURIComponent(q);
      }
      return $.ajax({
        url: uri,
        type: "get",
        success: (function(self) {
          return function() {
            var args;

            args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
            self.success.apply(self, args);
            if (typeof self.complete !== "undefined") {
              return self.complete.apply(self, args);
            }
          };
        })(this),
        error: (function(self) {
          return function() {
            var args;

            args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
            return self.error.apply(self, args);
          };
        })(this)
      });
    };

    Source.prototype.update = function(buffer, success, type) {
      var error, show;

      if (success == null) {
        success = null;
      }
      if (type == null) {
        type = "put";
      }
      if (success == null) {
        success = function() {};
      }
      show = (function(x) {
        return function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          return x.select();
        };
      })(this);
      error = (function(x) {
        return function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          return x.error.apply(x, a);
        };
      })(this);
      return $.ajax({
        url: "/content",
        type: type,
        data: JSON.stringify(buffer),
        success: function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          success.apply(null, a);
          return show();
        },
        error: error
      });
    };

    Source.prototype.create = function(buffer, success) {
      if (success == null) {
        success = null;
      }
      return this.update(buffer, success, "post");
    };

    Source.prototype.destroy = function(id, success) {
      var error, show;

      if (success == null) {
        success = null;
      }
      if (success == null) {
        success = function() {};
      }
      show = (function(x) {
        return function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          return x.select();
        };
      })(this);
      error = (function(x) {
        return function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          return x.error.apply(x, a);
        };
      })(this);
      return $.ajax({
        url: "/content/" + encodeURIComponent(id),
        type: "delete",
        success: function() {
          var a;

          a = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          success.apply(null, a);
          return show();
        },
        error: error
      });
    };

    Source.prototype.success = function(data, status) {
      var collections, k, t, v, _ref, _ref1, _results;

      collections = {};
      _ref = this.collections;
      for (k in _ref) {
        v = _ref[k];
        v([]);
      }
      _ref1 = this.entities;
      for (k in _ref1) {
        v = _ref1[k];
        if (typeof data[k] === "undefined") {
          delete this.entities[k];
          v(null);
        }
      }
      for (k in data) {
        v = data[k];
        t = k.split("#")[0];
        v = ko.mapping.fromJS(v);
        this.entity(k)(v);
        if (typeof collections[t] === "undefined") {
          collections[t] = [];
        }
        collections[t].push(v);
      }
      _results = [];
      for (k in collections) {
        v = collections[k];
        _results.push(this.collection(k)(v));
      }
      return _results;
    };

    Source.prototype.error = function(xhr, status, exception) {
      return alert("ERROR: " + status + " => " + exception);
    };

    return Source;

  })();

  this.content = {
    source: Source
  };

}).call(this);
