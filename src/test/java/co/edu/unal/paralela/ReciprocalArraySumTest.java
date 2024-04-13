package co.edu.unal.paralela;

import java.util.Random;
import junit.framework.TestCase;

public class ReciprocalArraySumTest extends TestCase {
    final static private int REPEATS = 60;

    private static int getNCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    private double[] createArray(final int N) {
        final double[] input = new double[N];
        final Random rand = new Random(314);
        for (int i = 0; i < N; i++) {
            input[i] = rand.nextInt(100) + 1;  // Ensuring non-zero elements to avoid division by zero
        }
        return input;
    }

    private double seqArraySum(final double[] input) {
        double sum = 0;
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }
        return sum;
    }

    private double parTestHelper(final int N, final boolean useManyTaskVersion, final int ntasks) {
        final double[] input = createArray(N);
        final double correct = seqArraySum(input);
        double sum;
        if (useManyTaskVersion) {
            sum = ReciprocalArraySum.parManyTaskArraySum(input, ntasks);
        } else {
            assert ntasks == 2;
            sum = ReciprocalArraySum.parArraySum(input);
        }
        final double err = Math.abs(sum - correct);
        final String errMsg = String.format("Result mismatch for N = %d, expected = %f, calculated = %f, absolute error = %f", N, correct, sum, err);
        assertTrue(errMsg, err < 1E-2);

        final long seqStartTime = System.currentTimeMillis();
        for (int r = 0; r < REPEATS; r++) {
            seqArraySum(input);
        }
        final long seqEndTime = System.currentTimeMillis();

        final long parStartTime = System.currentTimeMillis();
        for (int r = 0; r < REPEATS; r++) {
            if (useManyTaskVersion) {
                ReciprocalArraySum.parManyTaskArraySum(input, ntasks);
            } else {
                ReciprocalArraySum.parArraySum(input);
            }
        }
        final long parEndTime = System.currentTimeMillis();

        final long seqTime = (seqEndTime - seqStartTime) / REPEATS;
        final long parTime = (parEndTime - parStartTime) / REPEATS;

        // Testing for performance improvement
        assertTrue("Parallel implementation should be faster", parTime < seqTime);

        return (double)seqTime / (double)parTime;
    }

    public void testParSimpleTwoMillion() {
        final double speedup = parTestHelper(2_000_000, false, 2);
        assertTrue("Expected some improvement, speedup: " + speedup, speedup > 1);
    }
    

    public void testParSimpleTwoHundredMillion() {
        final double speedup = parTestHelper(200_000_000, false, 2);
        assertTrue("Expected speedup to be greater than 1", speedup > 1);
    }

    public void testParManyTaskTwoMillion() {
        final int ncores = getNCores();
        final double speedup = parTestHelper(2_000_000, true, ncores);
        assertTrue("Expected speedup to be greater than 1", speedup > 1);
    }

    public void testParManyTaskTwoHundredMillion() {
        final int ncores = getNCores();
        final double speedup = parTestHelper(200_000_000, true, ncores);
        assertTrue("Expected speedup to be greater than 1", speedup > 1);
    }
}
