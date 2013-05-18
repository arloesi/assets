(function() {
  ko.bindingHandlers.init = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
      var f;

      f = ko.utils.unwrapObservable(valueAccessor());
      return $(function() {
        return f(viewModel);
      });
    }
  };

  ko.bindingHandlers.update = {
    update: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
      var f;

      f = ko.utils.unwrapObservable(valueAccessor());
      return $(function() {
        return f(viewModel);
      });
    }
  };

  ko.bindingHandlers.datepicker = {
    init: function(element, valueAccessor, allBindingsAccessor) {
      var options;

      options = allBindingsAccessor().datepickerOptions || {};
      $(element).datepicker(options);
      ko.utils.registerEventHandler(element, "change", function() {
        var observable;

        observable = valueAccessor();
        return observable($(element).datepicker("getDate"));
      });
      return ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
        return $(element).datepicker("destroy");
      });
    },
    update: function(element, valueAccessor) {
      var current, value;

      value = ko.utils.unwrapObservable(valueAccessor());
      if (String(value).indexOf('/Date(') === 0) {
        value = new Date(parseInt(value.replace(/\/Date\((.*?)\)\//gi, "$1")));
      }
      current = $(element).datepicker("getDate");
      if (value - current !== 0) {
        return $(element).datepicker("setDate", value);
      }
    }
  };

  ko.bindingHandlers.enter = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
      var enter, handler;

      enter = ko.utils.unwrapObservable(valueAccessor());
      handler = function(event) {
        if (event.keyCode === 13) {
          enter(viewModel, event);
        }
        return true;
      };
      return ko.utils.registerEventHandler(element, "keypress", handler);
    }
  };

}).call(this);
