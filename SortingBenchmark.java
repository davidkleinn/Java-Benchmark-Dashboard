import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SortingBenchmark {
    private static final String OUTPUT_FILE_NAME = "benchmark_results.csv";
    private static final int[] DEFAULT_DATA_SIZES = {5_000, 10_000, 50_000};
    private static final int[] THREAD_CONFIGURATIONS = {2, 4, 8, 16};
    private static final int DEFAULT_SAMPLES = 5;

    @FunctionalInterface
    private interface SortRunner {
        void sort(int[] data, int threads);
    }

    private static final Map<String, SortRunner> SEQUENTIAL_ALGORITHMS = new LinkedHashMap<>();
    private static final Map<String, SortRunner> PARALLEL_ALGORITHMS = new LinkedHashMap<>();

    static {
        SEQUENTIAL_ALGORITHMS.put("Bubble Sort", (data, threads) -> SortingAlgorithms.bubbleSort(data));
        SEQUENTIAL_ALGORITHMS.put("Insertion Sort", (data, threads) -> SortingAlgorithms.insertionSort(data));
        SEQUENTIAL_ALGORITHMS.put("Merge Sort", (data, threads) -> SortingAlgorithms.mergeSort(data, 0, data.length - 1));
        SEQUENTIAL_ALGORITHMS.put("Quick Sort", (data, threads) -> SortingAlgorithms.quickSort(data, 0, data.length - 1));

        PARALLEL_ALGORITHMS.put("Merge Sort", SortingAlgorithms::parallelMergeSort);
        PARALLEL_ALGORITHMS.put("Quick Sort", SortingAlgorithms::parallelQuickSort);
    }

    public static int[] generateRandomArray(int size, long seed) {
        Random random = new Random(seed);
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(size);
        }
        return array;
    }

    public static boolean isSorted(int[] data) {
        for (int i = 1; i < data.length; i++) {
            if (data[i - 1] > data[i]) return false;
        }
        return true;
    }

    private static long measureSort(int[] originalData, SortRunner runner, int threadCount) {
        int[] data = Arrays.copyOf(originalData, originalData.length);

        long start = System.nanoTime();
        runner.sort(data, threadCount);
        long end = System.nanoTime();

        if (!isSorted(data)) {
            throw new IllegalStateException("Sorting failed for array size " + originalData.length);
        }
        return TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    private static int[] parseDataSizes(String[] args) {
        if (args.length == 0 || args[0].isBlank()) {
            return DEFAULT_DATA_SIZES;
        }

        String[] parts = args[0].split(",");
        int[] sizes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            sizes[i] = Integer.parseInt(parts[i].trim());
        }
        return sizes;
    }

    private static int parseSamples(String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            return DEFAULT_SAMPLES;
        }
        return Integer.parseInt(args[1].trim());
    }

    private static boolean shouldRunAlgorithm(String[] args, String algorithm) {
        if (args.length < 3 || args[2].isBlank() || "all".equalsIgnoreCase(args[2].trim())) {
            return true;
        }

        String normalizedAlgorithm = normalize(algorithm);
        for (String selected : args[2].split(",")) {
            String normalizedSelected = normalize(selected);
            if (normalizedAlgorithm.contains(normalizedSelected)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text.toLowerCase().replace(" ", "").replace("sort", "");
    }

    public static void main(String[] args) {
        int[] dataSizes = parseDataSizes(args);
        int samples = parseSamples(args);

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE_NAME))) {
            writer.println("Algorithm,DataSize,ExecutionMode,Threads,Sample,TimeMs,Sorted");

            for (int dataSize : dataSizes) {
                System.out.println("Generating data: " + dataSize + " elements");
                int[] baseData = generateRandomArray(dataSize, 31L * dataSize);

                for (Map.Entry<String, SortRunner> algorithm : SEQUENTIAL_ALGORITHMS.entrySet()) {
                    if (!shouldRunAlgorithm(args, algorithm.getKey())) continue;
                    for (int sample = 1; sample <= samples; sample++) {
                        long elapsedTime = measureSort(baseData, algorithm.getValue(), 1);
                        writer.printf("%s,%d,Sequential,1,%d,%d,true%n",
                                algorithm.getKey(), dataSize, sample, elapsedTime);
                    }
                }

                for (Map.Entry<String, SortRunner> algorithm : PARALLEL_ALGORITHMS.entrySet()) {
                    if (!shouldRunAlgorithm(args, algorithm.getKey())) continue;
                    for (int threadCount : THREAD_CONFIGURATIONS) {
                        for (int sample = 1; sample <= samples; sample++) {
                            long elapsedTime = measureSort(baseData, algorithm.getValue(), threadCount);
                            writer.printf("%s,%d,Parallel,%d,%d,%d,true%n",
                                    algorithm.getKey(), dataSize, threadCount, sample, elapsedTime);
                        }
                    }
                }
            }

            System.out.println("Benchmark completed. File generated: " + OUTPUT_FILE_NAME);
        } catch (IOException e) {
            System.err.println("Could not write the output file: " + e.getMessage());
        }
    }
}
