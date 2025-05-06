import be.tarsos.dsp.util.fft.FFT;


public class SpectrogramProcessor implements ManualAudioProcessor {
    private final RealTimeSpectrogram spectrogram;
    private final FFT fft;
    private final float[] amplitudes;

    public SpectrogramProcessor(RealTimeSpectrogram spectrogram, int bufferSize) {
        this.spectrogram = spectrogram;
        this.fft = new FFT(bufferSize);
        this.amplitudes = new float[bufferSize / 2];
    }

    @Override
    public void process(float[] floatBuffer) {
        fft.forwardTransform(floatBuffer);
        fft.modulus(floatBuffer, amplitudes);
        spectrogram.updateSpectrogram(amplitudes);
    }
}
