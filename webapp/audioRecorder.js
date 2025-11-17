// TODO deploy application, because MediaStream-API does not work for unsafe contexts (http). It needs to be https or else
//      navigator.mediaDevices.getUserMedia is undefined

document.addEventListener("DOMContentLoaded", function () {
    const startButton = document.getElementById("startRecording");
    const stopButton = document.getElementById("stopRecording");

    let audioContext;
    let mediaStream;
    let processorNode;
    let socket;

    startButton.addEventListener("click", async function () {
        console.log("Start Recording clicked");
        startButton.disabled = true;
        stopButton.disabled = false;
        await startRecording();
    });

    stopButton.addEventListener("click", function () {
        console.log("Stop Recording clicked");
        stopRecording();
        startButton.disabled = false;
        stopButton.disabled = true;
    });

    /**
     * Start recording the audio from the microphone and sends it via the websocket to DialogOS
     * @returns {Promise<void>}
     */
    async function startRecording() {
        // FIXME could be a problem if the recording device doesn´t have actually 48khz
        //  (it´s the most used one, but not all capturing devices have 48khz)
        audioContext = new AudioContext({sampleRate: 16000});
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });

        // Load Workletprocess to process audio to 16 bit pcm (sphinx needs it)
        await audioContext.audioWorklet.addModule("pcmProcessor.js");

        const source = audioContext.createMediaStreamSource(mediaStream);
        processorNode = new AudioWorkletNode(audioContext, "pcm-processor");

        processorNode.port.onmessage = (event) => {
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(event.data);
            }
        };

        source.connect(processorNode);
        processorNode.connect(audioContext.destination);

        // TODO change ip address (see first todo, deployment)
        socket = new WebSocket("ws://localhost:8080/audio-receive");
        socket.onopen = () => console.log("WebSocket connected.");
        socket.onerror = (event) => console.error("WebSocket error:", event);
        socket.onclose = () => console.log("WebSocket closed.");
    }

    function stopRecording() {
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.stop());
        }
        audioContext.close();
        if (socket) socket.close();
    }
});
