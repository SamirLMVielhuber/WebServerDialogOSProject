document.addEventListener("DOMContentLoaded", function () {
    // TODO change ip address
    console.log("JS_FILE_LOADED");
    const ws = new WebSocket("ws://192.168.178.20:8080/audio-stream");

    ws.binaryType = "arraybuffer";

    const volumeMeter = document.getElementById("volume-meter");

    ws.onmessage = function(event) {
        console.log("MESSAGE_RECEIVED");
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
        console.log("PLAY_AUDIO_CALLED");
        if (!audioContext) {
            //audioContext = new AudioContext({sampleRate: 48000});
            audioContext = new AudioContext();
            analyser = audioContext.createAnalyser();
            // basically determines the number of points to be shown in the meter
            analyser.fftSize = 256;
            dataArray = new Uint8Array(analyser.frequencyBinCount)
        }

        /*audioContext.decodeAudioData(audioData, function(buffer) {
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
        });*/
        function playAudio(audioData) {
            console.log("PLAY_AUDIO_CALLED");
            if (!audioContext) {
                audioContext = new AudioContext({ sampleRate: 48000 });

                analyser = audioContext.createAnalyser();
                analyser.fftSize = 256;
                dataArray = new Uint8Array(analyser.frequencyBinCount);
            }

            // Convert PCM Int16Array to Float32Array
            const pcmData = new Int16Array(audioData);

            const floatData = new Float32Array(pcmData.length);

            for (let i = 0; i < pcmData.length; i++) {
                floatData[i] = pcmData[i] / 32768.0;
            }

            // Create AudioBuffer manually
            const audioBuffer = audioContext.createBuffer(
                1,                  // mono
                floatData.length,
                48000               // sample rate
            );

            audioBuffer.copyToChannel(floatData, 0);

            // Stop previous audio
            if (audioSource) {
                audioSource.stop();
            }

            // Play
            audioSource = audioContext.createBufferSource();
            audioSource.buffer = audioBuffer;

            const gainNode = audioContext.createGain();

            audioSource.connect(gainNode);
            gainNode.connect(analyser);
            analyser.connect(audioContext.destination);

            audioSource.start();

            updateVolumeMeter();
        }
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
