import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

import static org.apache.commons.math3.util.FastMath.*;

public class SignalModel {
    static int Nbins = 2048;
    static int step = 1024;
    static int times = 48;
    static FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY);

    static double nf_g = 0.95;
    double min_variance = 100.0;
    static double [] window = new double[Nbins];
    static {for (int c1=0; c1<Nbins; c1++){window[c1] = sin(c1*PI/Nbins);};}

    static double [] noiseFloor = new double[step];

    static {
        Arrays.fill(noiseFloor, 0.0);
        ;}
    static GeometricMean Flatness_GM = new GeometricMean();
    static Mean Flatness_AM = new Mean();
    static HellingerDiverseMeasure hell = new HellingerDiverseMeasure();
    static int [] hellgrid = hell.getGrid(16, step);
    // Pre allocated spectrogram array and the transpose
    static double [][] spectrogramD = new double[times][step];
    static double [][] spectrogramT = new double[step][times];
    public static double computeFlatness(double [] vec, int start, int end, int len){
        double minv = new ArrayRealVector(vec).getMinValue();
        for (int c1=0; c1 < len; c1++){
            vec[c1] -= minv-1.0;
        }
        double gm = Flatness_GM.evaluate(vec, start, end);
        double am = Flatness_AM.evaluate(vec, start, end);
        return 1.0-(gm+0.0001)/(am+0.0001);
    }

    public static double [] computePowerSpectrum(double [] sig){
        Complex[] cy = fft.transform(sig, TransformType.FORWARD);
        double [] y = new double[step];
        for (int c1 = 0; c1 < step; c1++){
            y[c1] = 20.0*log10(cy[c1].abs()); //dB
        }
        return y;
    }
    public static double [][] computePowerSpectrogram(double[] sig){
        //int N = (int) floor(sig.length/step);
        int N = times;   // Using fixed time length
        for (int c1 = 0; c1 < N; c1++){
            double [] sel = Arrays.copyOfRange(sig, c1*step, c1*step+Nbins);
            for (int c2=0; c2<Nbins; c2++){
                sel[c2] *= window[c2];
            }
            spectrogramD[c1] = computePowerSpectrum(sel);
        }
        return spectrogramD;
    }

    public static void updateNoiseFloor(double [] sig){
        double [][] spectrogram = computePowerSpectrogram(sig);
        RealMatrix M = new Array2DRowRealMatrix(spectrogram);

        for (int c1=0; c1 <step; c1++)
        {
            double v = M.getColumnVector(c1).getMinValue();
            if (v!=v) v = 0.0;
            noiseFloor[c1] = nf_g*noiseFloor[c1]+(1.0-nf_g)*v;
        }
    }

    public static double maxFreqPeak(double [] vec){
        double maxVal = 0;
        double maxMean = 0;
        for (int c1 = 1; c1 < 800; c1++){
            maxMean += vec[c1];
            if (vec[c1] > maxVal){
                maxVal = vec[c1];
            }
        }
        maxMean /= 800;
        maxVal -= maxMean;

        return 2.0/(1.0+Math.exp(-maxVal/10.0))-1.0;
    }
    public static double [] computeTfFlatness(double [] sig){
        spectrogramD = computePowerSpectrogram(sig);
        double [] timeSum = new double[times];
        double [] freqMax = new double[step];
        Arrays.fill(freqMax, 0.0);
        Arrays.fill(timeSum, 0.0);;
        for (int c1=0; c1<times; c1++){
            for (int c2 = 0; c2 < step; c2++){
                spectrogramT[c2][c1] = spectrogramD[c1][c2]-noiseFloor[c2];
                timeSum[c1] += spectrogramT[c2][c1];
                if (spectrogramT[c2][c1]>freqMax[c2]){
                    freqMax[c2] = spectrogramT[c2][c1];
                }
            }
        }

        double [] measures = new double[3];
        measures[0] = computeFlatness(timeSum, 1, times-1, times);
        measures[1] = maxFreqPeak(freqMax);
        measures[2] = hell.hellingerSeries(spectrogramT, hellgrid);
        return measures;
    }
    public static double [] getNoiseFloor(){
        return noiseFloor;
    }
}
