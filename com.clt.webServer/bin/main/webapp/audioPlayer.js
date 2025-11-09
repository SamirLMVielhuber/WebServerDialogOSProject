window.addEventListener("error", (e) => {
  console.error("Global error:", e.message);
  logStatus("ERROR: " + e.message);
});

let audioContext = null;
let analyser = null;
let dataArray = null;

document.addEventListener("DOMContentLoaded", function () {
  const streamLight = document.getElementById("light-audio-stream");
  const volumeMeter = document.getElementById("volume-meter");

  streamLight.addEventListener("click", function () {
    if (window.audioApp.wsAudioStream && window.audioApp.wsAudioStream.readyState === WebSocket.OPEN) {
      logStatus("Closing audio stream WebSocket");
      window.audioApp.wsAudioStream.close();
    } else {
      connectAudioStreamWebSocket();
    }
  });

  function connectAudioStreamWebSocket() {
    const port = window.audioApp.inputPort;
    logStatus("Connecting to audio stream...\n   to Port" + port);
    const ws = new WebSocket(`ws://localhost:${port}/audio-stream?userId=${encodeURIComponent(window.audioApp.userId)}`);
  
    ws.binaryType = "arraybuffer";
    window.audioApp.wsAudioStream = ws;

    ws.onopen = () => {
        logStatus("Audio stream WebSocket connected");
        logStatus("Port: " + port);
        setLight("light-audio-stream", "green");
    };

    ws.onerror = (e) => {
        logStatus("Audio stream WebSocket error: " + (e.message || e));
        logStatus("Port: " + port);
        setLight("light-audio-stream", "red");
    };

    ws.onclose = () => {
        logStatus("Audio stream WebSocket closed");
        setLight("light-audio-stream", "red");
    };

    ws.onmessage = (event) => {
        playAudio(event.data);
    };
  }

  function playAudio(arrayBuffer) {
    const pcmData = new Int16Array(arrayBuffer);
    const float32Data = new Float32Array(pcmData.length);

    for (let i = 0; i < pcmData.length; i++) {
      float32Data[i] = pcmData[i] / 32768;
    }

    if (!audioContext) {
      audioContext = new AudioContext({ sampleRate: 16000 });
    }

    const buffer = audioContext.createBuffer(1, float32Data.length, 16000);
    buffer.copyToChannel(float32Data, 0);

    const source = audioContext.createBufferSource();
    source.buffer = buffer;

    const gainNode = audioContext.createGain();
    const localAnalyser = audioContext.createAnalyser();
    localAnalyser.fftSize = 256;

    const localDataArray = new Uint8Array(localAnalyser.frequencyBinCount);

    source.connect(gainNode);
    gainNode.connect(localAnalyser);
    localAnalyser.connect(audioContext.destination);

    source.start();

    updateVolumeMeter(localAnalyser, localDataArray);
  }

  function updateVolumeMeter(analyserInstance, dataArr) {
    function analyze() {
      analyserInstance.getByteFrequencyData(dataArr);
      let sum = dataArr.reduce((a, b) => a + b, 0);
      let average = sum / dataArr.length;
      const volumeMeter = document.getElementById("volume-meter");
      if (volumeMeter) {
        volumeMeter.value = Math.min(average, 100);
      }
      requestAnimationFrame(analyze);
    }
    analyze();
  }
});
