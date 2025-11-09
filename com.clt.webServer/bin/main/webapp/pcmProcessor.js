class PCMProcessor extends AudioWorkletProcessor {
    constructor() {
        super();
        this.resampleRatio = 48000 / 16000; // 48 kHz to 16 kHz resampling ratio
        this.buffer = [];
    }

    // Method is continuously called by the browser
    process(inputs, outputs, parameters) {
        // Input channel (mono)
        const input = inputs[0];
        if (input.length > 0) {
            const inputChannel = input[0]; // Mono audio channel

            // Convert to 16-bit PCM and downsample
            for (let i = 0; i < inputChannel.length; i++) {
                if (i % this.resampleRatio === 0) {
                    const sample = this.floatTo16BitPCM(inputChannel[i]);
                    this.buffer.push(sample);
                }
            }

            // Send the resampled 16-bit PCM data to the main thread & reset buffer
            this.port.postMessage(new Int16Array(this.buffer));
            this.buffer = [];
        }

        return true;
    }

    /**
     * Convert float to 16-bit signed PCM
     * @param floatSample data in float
     * @returns {number} data in 16-bit signed PCM
     */
    floatTo16BitPCM(floatSample) {
        const clampedSample = Math.max(-1, Math.min(1, floatSample));
        return clampedSample < 0 ? clampedSample * 0x8000 : clampedSample * 0x7FFF;
    }
}

registerProcessor("pcm-processor", PCMProcessor);
