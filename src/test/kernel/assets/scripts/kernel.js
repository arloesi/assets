(function() {
  this.encodeParams = function(i) {
    var k, r, v;

    r = "";
    for (k in i) {
      v = i[k];
      if (r !== "") {
        r += "&";
      }
      r += encodeURIComponent(k) + "=" + encodeURIComponent(v);
    }
    return r;
  };

}).call(this);
