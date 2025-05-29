import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

//Sketches spectrogram for GUI

public class RealTimeSpectrogram extends JPanel {

    private final BufferedImage spectrogram;
    private final int width, height;

    public RealTimeSpectrogram(int width, int height) {
        this.width = width;
        this.height = height;
        this.spectrogram = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void updateSpectrogram(float[] fftMagnitudes) {
        Graphics g = spectrogram.getGraphics();
        g.drawImage(spectrogram, -1, 0, null); 

        for (int y = 0; y < height; y++) {
            int bin = y * fftMagnitudes.length / height;
            float magnitude = fftMagnitudes[bin];
            int color = infernoColor(magnitude);
            spectrogram.setRGB(width - 1, height - 1 - y, color);
        }

        repaint();
    }

    private int infernoColor(float value) {
        float dB = 20 * (float) Math.log10(value + 1e-6f);
        float norm = Math.min(1f, Math.max(0f, (dB + 100f) / 100f));

        float r = (float) Math.pow(norm, 1.5f);
        float g = (float) Math.pow(norm, 3.0f);
        float b = (float) Math.pow(1.0 - norm, 2.0f) * 0.9f;

        r = Math.min(1f, r);
        g = Math.min(1f, g);
        b = Math.min(1f, b);

        return new Color(r, g, b).getRGB();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(spectrogram, 0, 0, getWidth(), getHeight(), null);
    }
}
