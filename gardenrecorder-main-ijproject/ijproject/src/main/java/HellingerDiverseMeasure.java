
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.linear.*;
import java.util.*;
import java.util.stream.*;

public class HellingerDiverseMeasure {
    static int[] getGrid(int bins, int N) {
        double[] b = IntStream.range(1, bins + 1).mapToDouble(i -> i).toArray();
        double h = Math.log(N) / bins;
        return Arrays.stream(b).map(val -> Math.exp(h * val)).mapToInt(val -> (int) val).toArray();
    }

    static List<Double[]> gridStatsMat(double[][] sp, int[] grid) {
        int gapSize = 2;
        List<Double[]> res = new ArrayList<>();
        for (int c1 = 0; c1 < grid.length - gapSize; c1++) {
            int st = grid[c1];
            int ed = grid[c1 + gapSize];
            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (int i = 0; i < sp[1].length; i++) {
                for (int j = st; j < ed; j++) {
                    stats.addValue(sp[j][i]);
                }
            }
            Double[] statArray = {stats.getMean(), stats.getVariance()};
            res.add(statArray);
        }
        return res;
    }

    static double hellingerDivergence(Double[] s1, Double[] s2) {
        double sws = s1[1] + s2[1];
        double e = -Math.pow(s1[0] - s2[0], 2) / (4 * sws);
        return 1 - Math.sqrt((2 * Math.sqrt(s1[1] * s2[1])) / sws) * Math.exp(e);
    }

    static double hellingerSeries(double[][] sp, int[] grid) {
        List<Double[]> r = gridStatsMat(sp, grid);
        double maxValue = 0;
        double maxBox = 0;
        int maxInd = 0;
        double v = 0;
        // Neighbors
        for (int c1 = 0; c1 < r.size() - 1; c1++) {
            v = hellingerDivergence(r.get(c1), r.get(c1 + 1));
            if (v > maxValue){
                maxValue = v;
            }
            if (r.get(1)[0]>maxBox){
                maxBox = r.get(1)[0];
                maxInd = c1;
            }
        }
        // Compare max to all
        for (int c1 = 0; c1 < r.size() - 1; c1++) {
            if (c1 != maxInd) {
                v = hellingerDivergence(r.get(maxInd), r.get(c1));
                if (v > maxValue) {
                    maxValue = v;
                }
            }
        }
        return maxValue;
    }
}

