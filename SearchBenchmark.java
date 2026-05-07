import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SearchBenchmark {

    // ==========================================
    // 1. Sequential Search
    // ==========================================
    public static boolean sequentialSearch(int[] array, int target) {
        for (int value : array) {
            if (value == target) return true;
        }
        return false;
    }

    // ==========================================
    // 2. Parallel Search Task
    // ==========================================
    static class SearchTask implements Callable<Boolean> {
        private final int[] array;
        private final int start;
        private final int end;
        private final int target;

        public SearchTask(int[] array, int start, int end, int target) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.target = target;
        }

        @Override
        public Boolean call() {
            for (int i = start; i < end; i++) {
                if (array[i] == target) return true;
            }
            return false;
        }
    }

    // ==========================================
    // 3. Parallel Search
    // ==========================================
    public static boolean parallelSearch(int[] array, int target, int threadCount)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<SearchTask> tasks = new ArrayList<>();

        int chunkSize = array.length / threadCount;

        for (int i = 0; i < threadCount; i++) {
            int start = i * chunkSize;
            int end = (i == threadCount - 1) ? array.length : start + chunkSize;
            tasks.add(new SearchTask(array, start, end, target));
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        for (Future<Boolean> result : results) {
            if (result.get()) return true;
        }
        return false;
    }

    // ==========================================
    // 4. Data Generation
    // ==========================================
    public static int[] generateRandomArray(int size) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(size);
        }
        array[size - 1] = -1;
        return array;
    }

    public static long runSequentialSearch(int[] data) {
        long start = System.nanoTime();
        sequentialSearch(data, -1);
        long end = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    public static long runParallelSearch(int[] data, int threadCount)
            throws InterruptedException, ExecutionException {
        long start = System.nanoTime();
        parallelSearch(data, -1, threadCount);
        long end = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    // ==========================================
    // 5. Benchmark Runner
    // ==========================================
    public static void main(String[] args) throws Exception {
        String outputFileName = "benchmark_results.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            writer.println("DataSize,ExecutionMode,Threads,Sample,TimeMs");

            int[] dataSizes = {10_000_000, 50_000_000, 100_000_000};
            int[] threadConfigurations = {2, 4, 8, 16};
            int samples = 5;

            for (int dataSize : dataSizes) {
                int[] data = generateRandomArray(dataSize);

                for (int sample = 1; sample <= samples; sample++) {
                    long elapsedTime = runSequentialSearch(data);
                    writer.printf("%d,Sequential,1,%d,%d%n", dataSize, sample, elapsedTime);
                }

                for (int threadCount : threadConfigurations) {
                    for (int sample = 1; sample <= samples; sample++) {
                        long elapsedTime = runParallelSearch(data, threadCount);
                        writer.printf("%d,Parallel,%d,%d,%d%n", dataSize, threadCount, sample, elapsedTime);
                    }
                }
            }
            System.out.println("Benchmark completed. File generated: " + outputFileName);
        } catch (IOException e) {
            System.err.println("Could not write the output file: " + e.getMessage());
        }
    }
}
