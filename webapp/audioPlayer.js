document.addEventListener("DOMContentLoaded", function () {
    // TODO change ip address
    const ws = new WebSocket("ws://192.168.178.20:8080/audio-stream");

    ws.binaryType = "arraybuffer";

    const volumeMeter = document.getElementById("volume-meter");

    ws.onmessage = function(event) {
        const audioData = event.data;
        playAudio(audioData);
    };

    let audioContext;
    let audioSource;
    let analyser;
    let dataArray;

    /**
     * plays the received audio to the speakers
     * @param audioData the audio to play
     */
    function playAudio(audioData) {
        if (!audioContext) {
            //audioContext = new AudioContext({sampleRate: 48000});
            audioContext = new AudioContext();
            analyser = audioContext.createAnalyser();
            // basically determines the number of points to be shown in the meter
            analyser.fftSize = 256;
            dataArray = new Uint8Array(analyser.frequencyBinCount)
        }

        audioContext.decodeAudioData(audioData, function(buffer) {
            if (audioSource) {
                audioSource.stop();
            }

            audioSource = audioContext.createBufferSource();
            audioSource.buffer = buffer;
            const gainNode = audioContext.createGain();

            audioSource.connect(gainNode);
            gainNode.connect(analyser);
            analyser.connect(audioContext.destination);
            audioSource.start(0);

            updateVolumeMeter();
        });
    }

    /**
     * Used to dynamically update the volume meter for the webpage
     */
    function updateVolumeMeter() {
        if (!analyser) return;

        function analyze() {
            analyser.getByteFrequencyData(dataArray);
            let sum = dataArray.reduce((a, b) => a + b, 0);
            let average = sum / dataArray.length;

            // Scale to fit the meter (0-100 range)
            volumeMeter.value = Math.min(average, 100);

            requestAnimationFrame(analyze);
        }

        analyze();
    }
});
