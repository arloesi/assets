(function() {
  var Event, Service, commands, i, server, session, _i, _len,
    __slice = [].slice;

  session = $.cookie("session", void 0, {
    path: "/"
  });

  commands = [];

  server = new SockJS("/socket");

  server.onopen = function() {
    var i, _i, _len;

    console.log("Opened Socket");
    for (_i = 0, _len = commands.length; _i < _len; _i++) {
      i = commands[_i];
      server.send(i);
    }
    return commands = [];
  };

  server.onclose = function() {
    return console.log("Closed Socket");
  };

  server.onmessage = function(e) {
    var event, json, service;

    console.log("Message: " + e.data);
    json = JSON.parse(e.data);
    service = $.services[json.service];
    if (typeof service !== "undefined") {
      event = service.events[json.event];
      if (typeof event !== "undefined") {
        return event.send(json.params);
      }
    }
  };

  server.safeSend = function(i) {
    if (this.readyState !== WebSocket.OPEN) {
      console.log("Socket is not open yet.");
      return commands.push(i);
    } else {
      return this.send(i);
    }
  };

  Event = (function() {
    function Event(service, event) {
      this.subscribers = [];
      this.subscribe = function(f) {
        var params;

        console.log("subscribe: " + service.name + " => " + event.name);
        this.subscribers.push(f);
        if (this.subscribers.length === 1) {
          params = JSON.stringify({
            session: session,
            service: service.name,
            method: "subscribe",
            event: event.name
          });
          return server.safeSend(params);
        }
      };
      this.unsubscribe = function(f) {
        var params;

        this.subscribers.splice(this.subscribers.indexOf(f), 1);
        if (this.subscribers.length === 0) {
          params = JSON.stringify({
            session: session,
            service: service.name,
            method: "unsubscribe",
            event: event.name
          });
          return server.safeSend(params);
        }
      };
      this.send = function() {
        var args, i, _i, _len, _ref, _results;

        args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
        _ref = this.subscribers;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          i = _ref[_i];
          _results.push(i.apply(null, args));
        }
        return _results;
      };
    }

    return Event;

  })();

  Service = (function() {
    function Service(service) {
      var i, socket, _i, _j, _len, _len1, _ref, _ref1;

      socket = function(method) {
        return function(params) {
          return server.safeSend(JSON.stringify({
            session: session,
            service: service.name,
            method: method.name,
            params: params
          }));
        };
      };
      _ref = service.methods;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        i = _ref[_i];
        this[i.name] = socket(i);
      }
      this.events = {};
      _ref1 = service.events;
      for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
        i = _ref1[_j];
        this.events[i.name] = new Event(service, i);
      }
    }

    return Service;

  })();

  $.services = {};

  for (_i = 0, _len = __services.length; _i < _len; _i++) {
    i = __services[_i];
    $.services[i.name] = new Service(i);
  }

}).call(this);
