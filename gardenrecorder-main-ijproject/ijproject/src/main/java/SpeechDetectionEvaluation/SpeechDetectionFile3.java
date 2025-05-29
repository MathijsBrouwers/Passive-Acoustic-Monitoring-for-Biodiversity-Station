package SpeechDetectionEvaluation;

import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.Yin;


import java.io.File;

//Evaluates ratio of speech detected in folder

public class SpeechDetectionFile3 { //Based on energy and pitch and two subsequent checks
    private static boolean isSpeech = false;
    private static float detectedPitch = 0;
    private static int amountYes = 0;
    private static int amountNo = 0;
    private static double ratioSpeechDetected = 0;
    private static boolean previousIsSpeech = false;


    public static void main(String[] args) {

        String folderPath = "path_to_folder";



        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.out.println("Invalid folder path.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null || files.length == 0) {
            System.out.println("No WAV files found in the folder.");
            return;
        }

        for (File audioFile : files) {
            System.out.println("Processing file: " + audioFile.getName());
            processFile(audioFile);
        }
    }

private static void processFile(File audioFile) {
    int bufferSize = 1024;
    int overlap = 512;
    int sampleRate = 44100;

    try {
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, bufferSize, overlap);

        Yin yin = new Yin(sampleRate, bufferSize);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] buffer = audioEvent.getFloatBuffer();

                double energy = computeEnergy(buffer);
                boolean currentSpeech = energy > 0.01;
                
                PitchDetectionResult result = yin.getPitch(buffer);
                detectedPitch = result.getPitch();
                if (detectedPitch > 800) {
                    currentSpeech = false;
                }
                
                isSpeech = (currentSpeech && previousIsSpeech);
                
                System.out.println(isSpeech ? "Speech detected: yes, Pitch: " + detectedPitch + " Hz" : "Speech detected: no, Pitch: " + detectedPitch + " Hz");
                if (isSpeech) {
                    amountYes++;
                } else {
                    amountNo++;
                }
                previousIsSpeech = currentSpeech;

                return true;
            }

            @Override
            public void processingFinished() {
                System.out.println("Finished processing " + audioFile.getName());
                ratioSpeechDetected = (double) amountYes / (amountYes + amountNo);
                System.out.println("Ratio of Speech Detected: " + ratioSpeechDetected);

                amountYes = 0;
                amountNo = 0;
                previousIsSpeech = false;
            }
        });

        dispatcher.run();

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private static double computeEnergy(float[] buffer) {
        double sum = 0;
        for (float sample : buffer) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / buffer.length);
    }
}