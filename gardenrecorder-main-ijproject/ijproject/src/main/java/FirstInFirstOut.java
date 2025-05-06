import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;

import static org.apache.commons.math3.transform.DctNormalization.STANDARD_DCT_I;
import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.apache.commons.math3.util.FastMath.log10;

public class FirstInFirstOut {
    public class NoiseFloor {
        static int Nbins = 2048;
        static int step = 1024;
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY);

        public double min_variance = 100.0;

        static double [] window = new double[Nbins];
        static {for (int c1=0; c1<Nbins; c1++){window[c1] = sin(c1*PI/Nbins);};}

    }
    int FS = 48000;
    int NUM_CHANNELS = 2;
    enum States {IDLE, BACKGROUND, RECORDING, STORING};
    int MAX_BUFFER_LENGTH = 60*FS;
    int REC_FRAMES = 10;
    NoiseFloor NF = new NoiseFloor();
    double [][] buffer = new double[NUM_CHANNELS][MAX_BUFFER_LENGTH];

    int inp = 0;
    int outp = MAX_BUFFER_LENGTH/2;
    static States state = States.IDLE;
    static int recording_counter = 0;

    public static double [] hellMeasures = new double[3];
    public static double hellTh1 = 0.5;
    public static double hellTh2 = 0.5;
    public static double hellTh3 = 0.5;
    public static double varianceGate = 20;
    public StoreData dataWriter = new StoreData();
    public RecordingLog log = new RecordingLog();
    static States previousState = States.IDLE;
    public void initialize(String pathName) throws IOException {
            String globalPath = "C:\\Data\\er_path\\";
            File filePath = new File(globalPath);
            if (!filePath.exists()) {filePath.mkdir();}
            Arrays.fill(buffer[0], 0.01);
            Arrays.fill(buffer[1], 0.01);
            dataWriter.setPathPrefix(globalPath+pathName);
            log.initializeJsonWriter(globalPath+pathName, pathName);
    }
    public void close() throws IOException {
        log.closeJsonWriter();
    }

    public double [][] bytes_to_floats(byte [] bytes, int length, int channels) {
        double[][] audioData = new double[channels][length];
        int i = 0;
        for (int c1 = 0; c1 < length; c1++) {
            for (int c2=0; c2 < channels; c2++) {
                /* First byte is MSB (high order) */
                int MSB = (int) bytes[i++];
                /* Second byte is LSB (low order) */
                int LSB = (int) bytes[i++];
                audioData[c2][c1] = (double) (MSB << 8 | (255 & LSB));
            }
        }
        return audioData;
    }

   public double [] getSignal(int refinp, int length, int channel){
       int bufferIndex = inp-refinp;
       if (bufferIndex<0){bufferIndex += MAX_BUFFER_LENGTH;}
       double [] out = new double[length];
       for (int c1 = 0; c1 < length; c1++) {
           out[c1] = buffer[channel][bufferIndex];
           if (++bufferIndex==MAX_BUFFER_LENGTH){
               bufferIndex = 0;
           }
       }
       return out;
   }

   public boolean signal_energy_variance(double [][] signal, int len, int channels){
        double var0 = StatUtils.variance(signal[0]);
        double var1 = StatUtils.variance(signal[1]);

        double var = 0;
        if (var0>var1) var = 20*Math.log(var0+0.0001); else var=20*Math.log(var1+0.0001);

       new Thread(() -> {
            double [] sig = getSignal(REC_FRAMES*len, 58*1024, 0);
            SignalModel.updateNoiseFloor(sig);
        }).start();

        if (var>0.0) {
            NF.min_variance = 0.98 * NF.min_variance + 0.02 * var;
        }
        if ((var > NF.min_variance + varianceGate)&(recording_counter == 0)){
            state = States.RECORDING;
            recording_counter = REC_FRAMES;
            return true;
        }
        else{
            if (state == States.RECORDING){recording_counter--;}
            else {return false;}
            if (recording_counter==0){
                state = States.STORING;
                new Thread(() -> {
                    String [] storedFileName = new String[0];
                    double [] sig0 = getSignal(REC_FRAMES*len, 48*1024, 0);
                    hellMeasures = SignalModel.computeTfFlatness(sig0);
                    System.out.println(hellMeasures[0]+"|"+hellMeasures[1]+"|"+hellMeasures[2]);
                    if ((hellMeasures[0]>hellTh1)|(hellMeasures[1]>hellTh2)|(hellMeasures[2]>hellTh3)) {
                        double [] measures = new double[]{hellMeasures[0], hellTh1, hellMeasures[1], hellTh2,
                                hellMeasures[2], hellTh3};
                        try {
                            storedFileName = dataWriter.storeAudio(buffer, MAX_BUFFER_LENGTH,
                                    inp, REC_FRAMES * len + len, channels);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        log.audioRecordingEntry("test", storedFileName[1], storedFileName[0], measures);
                    }
                    else{
                        System.out.println("Threshold not triggered. Did not store the data");
                    }
                    state = States.IDLE;
                    }).start();
            }
        }
       // System.out.println(state);
       return false;
    }




    /// The main function for writing data to the buffer
    public void write_bytes_to_buffer(byte [] data, int channels) {
        int length = (int)(data.length/(2*channels));
        double[][] signal = bytes_to_floats(data, length, channels);
        for (int c1=0; c1<length; c1++){
            for (int c2=0; c2<channels; c2++) {
                buffer[c2][inp] = signal[c2][c1];
            }
            if (++inp>=MAX_BUFFER_LENGTH){
                inp = 0;
            }
        }
        boolean triggered = signal_energy_variance(signal, length, channels);
        if (state != previousState){
            System.out.println(state);
            previousState = state;
        }

    }

}
