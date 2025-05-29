import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

//Processes speech detection

public class SpeechDetectionProcessor implements ManualAudioProcessor {
    private  boolean isSpeech = false;
    private final JLabel speechStatusLabel;
    private int aggressivenessLevel;

    private final Queue<Boolean> recentSpeechFlags = new LinkedList<>();
    private final int speechMemorySize = 5; 

    public SpeechDetectionProcessor(JLabel speechStatusLabel, int aggressivenessLevel) {
        this.speechStatusLabel = speechStatusLabel;
        this.aggressivenessLevel = aggressivenessLevel;
    }

    public void processWithPitch(float[] floatBuffer, float pitch) {
        double energy = computeEnergy(floatBuffer);
        //System.out.println("Agg: " + aggressivenessLevel);
        boolean currentSpeech = false;
        if (aggressivenessLevel > 0) {
            currentSpeech = energy > 0.01;
    
            if ((aggressivenessLevel == 2 || aggressivenessLevel == 3) && pitch > 600) {
                currentSpeech = false;
            }
    
            if (aggressivenessLevel == 3) {
                recentSpeechFlags.add(currentSpeech);
                if (recentSpeechFlags.size() > speechMemorySize) {
                    recentSpeechFlags.poll();  
                }
                int speechCount = 0;
                for (boolean b : recentSpeechFlags) {
                    if (b) speechCount++;
                }
                isSpeech = (speechCount >= (speechMemorySize / 2) + 1);
            } else {
                isSpeech = currentSpeech;
            }
        } else {
            isSpeech = false;
        }

        SwingUtilities.invokeLater(() -> {
            speechStatusLabel.setText("Speech detected: " + (isSpeech ? "yes" : "no"));
            speechStatusLabel.setForeground(isSpeech ? Color.GREEN.darker() : Color.RED);
        });
    }
    

    @Override
    public void process(float[] floatBuffer) {
        System.out.println("Error: Use processWithPitch instead");
    }

    public boolean isSpeechDetected() {
        return isSpeech;
    }

    private static double computeEnergy(float[] buffer) {
        double sum = 0;
        for (float sample : buffer) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / buffer.length);
    }
}
