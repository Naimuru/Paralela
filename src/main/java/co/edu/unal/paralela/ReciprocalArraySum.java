package co.edu.unal.paralela;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Clase que contiene los métodos para implementar la suma de los recíprocos de un arreglo usando paralelismo.
 */
public final class ReciprocalArraySum {

    /**
     * Constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Calcula secuencialmente la suma de valores recíprocos para un arreglo.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }
        return sum;
    }

    /**
     * Calculates the sum of reciprocals using the ForkJoin framework by dividing the input array into two parts.
     * Adapts to available hardware resources by utilizing the common ForkJoinPool.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parArraySum(final double[] input) {
        ForkJoinPool pool = new ForkJoinPool();  // Utilizes the common pool which adapts to available hardware

        class SumTask extends RecursiveTask<Double> {
            private final int start;
            private final int end;
            private static final int SEQUENTIAL_THRESHOLD = 5000;  // Adjust based on profiling

            SumTask(int start, int end) {
                this.start = start;
                this.end = end;
            }

            @Override
            protected Double compute() {
                int length = end - start;
                if (length <= SEQUENTIAL_THRESHOLD) {
                    double localSum = 0;
                    for (int i = start; i < end; i++) {
                        localSum += 1 / input[i];
                    }
                    return localSum;
                } else {
                    int mid = start + length / 2;
                    SumTask left = new SumTask(start, mid);
                    SumTask right = new SumTask(mid, end);
                    left.fork();
                    double rightResult = right.compute();
                    double leftResult = left.join();
                    return leftResult + rightResult;
                }
            }
        }

        return pool.invoke(new SumTask(0, input.length));
    }

    /**
     * Extends parArraySum to use a specified number of tasks to compute the sum of reciprocals.
     *
     * @param input Arreglo de entrada
     * @param numTasks Number of tasks to use
     * @return The sum of the reciprocals of the array
     */
    protected static double parManyTaskArraySum(final double[] input, final int numTasks) {
        ForkJoinPool pool = new ForkJoinPool(numTasks);

        class SumTask extends RecursiveTask<Double> {
            private final int start;
            private final int end;

            SumTask(int start, int end) {
                this.start = start;
                this.end = end;
            }

            @Override
            protected Double compute() {
                if (end - start <= 1000) { // Adjust this threshold based on profiling
                    double localSum = 0;
                    for (int i = start; i < end; i++) {
                        localSum += 1 / input[i];
                    }
                    return localSum;
                } else {
                    int mid = start + (end - start) / 2;
                    SumTask left = new SumTask(start, mid);
                    SumTask right = new SumTask(mid, end);
                    left.fork();
                    double rightResult = right.compute();
                    double leftResult = left.join();
                    return leftResult + rightResult;
                }
            }
        }

        SumTask[] tasks = new SumTask[numTasks];
        for (int i = 0; i < numTasks; i++) {
            int start = getChunkStartInclusive(i, numTasks, input.length);
            int end = getChunkEndExclusive(i, numTasks, input.length);
            tasks[i] = new SumTask(start, end);
            tasks[i].fork();
        }

        double sum = 0;
        for (SumTask task : tasks) {
            sum += task.join();
        }
        return sum;
    }

    private static int getChunkSize(final int nChunks, final int nElements) {
        return (nElements + nChunks - 1) / nChunks;
    }

    private static int getChunkStartInclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    private static int getChunkEndExclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        return Math.min(end, nElements);
    }
}
