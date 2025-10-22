window.logStatus = function (message) {
  let logDiv = document.getElementById("debug-log");
  if (!logDiv) {
    logDiv = document.createElement("div");
    logDiv.id = "debug-log";
    logDiv.style.position = "fixed";
    logDiv.style.bottom = "0";
    logDiv.style.left = "0";
    logDiv.style.backgroundColor = "rgba(0,0,0,0.7)";
    logDiv.style.color = "white";
    logDiv.style.fontSize = "12px";
    logDiv.style.padding = "5px";
    logDiv.style.maxHeight = "200px";
    logDiv.style.overflowY = "auto";
    document.body.appendChild(logDiv);
  }
  const p = document.createElement("p");
  p.textContent = message;
  logDiv.appendChild(p);
  logDiv.scrollTop = logDiv.scrollHeight;
};

window.setLight = function (id, color) {
  const el = document.getElementById(id);
  if (el) el.style.backgroundColor = color;
};
