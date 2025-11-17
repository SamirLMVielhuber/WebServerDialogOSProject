// TODO deploy application, because MediaStream-API does not work for unsafe contexts (http). It needs to be https or else
//      navigator.mediaDevices.getUserMedia is undefined
window.addEventListener("error", (e) => {
  console.error("Global error:", e.message);
  logStatus("ERROR: " + e.message);
});

document.addEventListener("DOMContentLoaded", function () {
  const startBtn = document.getElementById("startRecording");
  const stopBtn = document.getElementById("stopRecording");
  const sendLight = document.getElementById("light-audio-send");

  sendLight.addEventListener("click", function () {
    if (window.audioApp.wsAudioSend && window.audioApp.wsAudioSend.readyState === WebSocket.OPEN) {
      logStatus("Closing audio-send WebSocket");
      window.audioApp.wsAudioSend.close();
    } else {
      connectAudioSendWebSocket();
    }
  });

  function connectAudioSendWebSocket() {
    const port = window.audioApp.inputPort;
    logStatus("Connecting to audio-receive WebSocket...\n   to Port" + port);
    const ws = new WebSocket(`ws://localhost:${port}/audio-receive?userId=${encodeURIComponent(window.audioApp.userId)}`);

    ws.binaryType = "arraybuffer";
    window.audioApp.wsAudioSend = ws;

    ws.onopen = () => {
      logStatus("Audio-send WebSocket connected");
      logStatus("Port: " + port);
      setLight("light-audio-send", "green");
    };

    ws.onerror = (e) => {
      logStatus("Audio-send WebSocket error: " + (e.message || e));
      logStatus("Port: " + port);
      setLight("light-audio-send", "red");
    };

    ws.onclose = () => {
      logStatus("Audio-send WebSocket closed");
      setLight("light-audio-send", "red");
    };
  }

  async function startRecording() {
    const socket = window.audioApp.wsAudioSend;

    if (!socket || socket.readyState !== WebSocket.OPEN) {
      logStatus("WebSocket not open – cannot start recording");
      return;
    }

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    //const context = new AudioContext({ sampleRate: 16000 });
    const context = new AudioContext();
    console.log("Actual AudioContext sampleRate:", audioContext.sampleRate);

    await context.audioWorklet.addModule("pcmProcessor.js");

    const source = context.createMediaStreamSource(stream);
    const processorNode = new AudioWorkletNode(context, "pcm-processor");

    processorNode.port.onmessage = (event) => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(event.data);
      }
    };

    source.connect(processorNode);
    processorNode.connect(context.destination);

    //Save to global state
    window.audioApp.audioContext = context;
    window.audioApp.processorNode = processorNode;
    window.audioApp.mediaStream = stream;

    logStatus("Recording started");
    startBtn.disabled = true;
    stopBtn.disabled = false;
  }

  function stopRecording() {
    const ctx = window.audioApp.audioContext;
    const processor = window.audioApp.processorNode;
    const stream = window.audioApp.mediaStream;
    const socket = window.audioApp.wsAudioSend;

    if (processor) {
      processor.disconnect();
      window.audioApp.processorNode = null;
    }
    if (ctx) {
      ctx.close();
      window.audioApp.audioContext = null;
    }
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      window.audioApp.mediaStream = null;
    }

    setLight("light-audio-send", socket && socket.readyState === WebSocket.OPEN ? "green" : "red");
    logStatus("Recording stopped");

    startBtn.disabled = false;
    stopBtn.disabled = true;
  }

  startBtn.addEventListener("click", startRecording);
  stopBtn.addEventListener("click", stopRecording);
});
