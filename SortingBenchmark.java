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

    /**
     * Naturezas de dados testadas:
     *   Random  – array aleatorio com semente fixa (cenario medio, mais comum)
     *   Sorted  – array ja ordenado crescentemente (melhor caso para Bubble/Insertion)
     *   Reverse – array em ordem decrescente (pior caso para Bubble/Insertion)
     */
    private static final String[] DATA_TYPES = {"Random", "Sorted", "Reverse"};

    /**
     * Bubble Sort e Insertion Sort no pior caso (Reverse) com arrays acima desse limite
     * levariam muitos minutos — O(n^2) com n >= 50 000. A combinacao e pulada e registrada
     * no console. Dados em ordem crescente (Sorted) sao mantidos pois representam o melhor
     * caso desses algoritmos (O(n)) e sao uteis para a analise comparativa.
     */
    private static final int SLOW_ALGO_REVERSE_LIMIT = 10_000;

    @FunctionalInterface
    private interface SortRunner {
        void sort(int[] data, int threads);
    }

    private static final Map<String, SortRunner> SEQUENTIAL_ALGORITHMS = new LinkedHashMap<>();
    private static final Map<String, SortRunner> PARALLEL_ALGORITHMS = new LinkedHashMap<>();

    static {
        SEQUENTIAL_ALGORITHMS.put("Bubble Sort",    (data, t) -> SortingAlgorithms.bubbleSort(data));
        SEQUENTIAL_ALGORITHMS.put("Insertion Sort", (data, t) -> SortingAlgorithms.insertionSort(data));
        SEQUENTIAL_ALGORITHMS.put("Merge Sort",     (data, t) -> SortingAlgorithms.mergeSort(data, 0, data.length - 1));
        SEQUENTIAL_ALGORITHMS.put("Quick Sort",     (data, t) -> SortingAlgorithms.quickSort(data, 0, data.length - 1));

        PARALLEL_ALGORITHMS.put("Merge Sort", SortingAlgorithms::parallelMergeSort);
        PARALLEL_ALGORITHMS.put("Quick Sort", SortingAlgorithms::parallelQuickSort);
    }

    // -----------------------------------------------------------------------
    // Geracao de dados
    // -----------------------------------------------------------------------

    public static int[] generateRandomArray(int size, long seed) {
        Random random = new Random(seed);
        int[] array = new int[size];
        for (int i = 0; i < size; i++) array[i] = random.nextInt(size);
        return array;
    }

    /** Array ja ordenado crescentemente — melhor caso para Bubble Sort e Insertion Sort. */
    public static int[] generateSortedArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) array[i] = i;
        return array;
    }

    /** Array em ordem decrescente — pior caso para Bubble Sort e Insertion Sort. */
    public static int[] generateReverseArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) array[i] = size - 1 - i;
        return array;
    }

    private static int[] baseDataFor(String dataType, int dataSize) {
        return switch (dataType) {
            case "Sorted"  -> generateSortedArray(dataSize);
            case "Reverse" -> generateReverseArray(dataSize);
            default        -> generateRandomArray(dataSize, 31L * dataSize);
        };
    }

    // -----------------------------------------------------------------------
    // Medicao
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Parsing de argumentos
    // -----------------------------------------------------------------------

    private static int[] parseDataSizes(String[] args) {
        if (args.length == 0 || args[0].isBlank()) return DEFAULT_DATA_SIZES;
        String[] parts = args[0].split(",");
        int[] sizes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) sizes[i] = Integer.parseInt(parts[i].trim());
        return sizes;
    }

    private static int parseSamples(String[] args) {
        if (args.length < 2 || args[1].isBlank()) return DEFAULT_SAMPLES;
        return Integer.parseInt(args[1].trim());
    }

    private static boolean shouldRunAlgorithm(String[] args, String algorithm) {
        if (args.length < 3 || args[2].isBlank() || "all".equalsIgnoreCase(args[2].trim())) return true;
        String normalizedAlgorithm = normalize(algorithm);
        for (String selected : args[2].split(",")) {
            if (normalizedAlgorithm.contains(normalize(selected))) return true;
        }
        return false;
    }

    /**
     * Retorna true quando a combinacao algoritmo + tamanho + tipo de dado seria
     * pratica e excessivamente lenta para executar no benchmark padrao.
     */
    private static boolean shouldSkip(String algorithm, int dataSize, String dataType) {
        if (!"Reverse".equals(dataType)) return false;
        if (dataSize <= SLOW_ALGO_REVERSE_LIMIT) return false;
        return algorithm.equals("Bubble Sort") || algorithm.equals("Insertion Sort");
    }

    private static String normalize(String text) {
        return text.toLowerCase().replace(" ", "").replace("sort", "");
    }

    // -----------------------------------------------------------------------
    // Main
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        int[] dataSizes = parseDataSizes(args);
        int samples = parseSamples(args);

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE_NAME))) {
            writer.println("Algorithm,DataSize,DataType,ExecutionMode,Threads,Sample,TimeMs,Sorted");

            for (int dataSize : dataSizes) {
                for (String dataType : DATA_TYPES) {
                    System.out.printf("Gerando dados: %d elementos / %s%n", dataSize, dataType);
                    int[] baseData = baseDataFor(dataType, dataSize);

                    // --- Versoes sequenciais ---
                    for (Map.Entry<String, SortRunner> entry : SEQUENTIAL_ALGORITHMS.entrySet()) {
                        String name = entry.getKey();
                        if (!shouldRunAlgorithm(args, name)) continue;
                        if (shouldSkip(name, dataSize, dataType)) {
                            System.out.printf("  [PULADO] %s + %s com %d elementos (O(n^2) no pior caso)%n",
                                    name, dataType, dataSize);
                            continue;
                        }
                        for (int sample = 1; sample <= samples; sample++) {
                            long elapsed = measureSort(baseData, entry.getValue(), 1);
                            writer.printf("%s,%d,%s,Sequential,1,%d,%d,true%n",
                                    name, dataSize, dataType, sample, elapsed);
                        }
                    }

                    // --- Versoes paralelas ---
                    for (Map.Entry<String, SortRunner> entry : PARALLEL_ALGORITHMS.entrySet()) {
                        String name = entry.getKey();
                        if (!shouldRunAlgorithm(args, name)) continue;
                        for (int threadCount : THREAD_CONFIGURATIONS) {
                            for (int sample = 1; sample <= samples; sample++) {
                                long elapsed = measureSort(baseData, entry.getValue(), threadCount);
                                writer.printf("%s,%d,%s,Parallel,%d,%d,%d,true%n",
                                        name, dataSize, dataType, threadCount, sample, elapsed);
                            }
                        }
                    }
                }
            }

            System.out.println("Benchmark concluido. Arquivo gerado: " + OUTPUT_FILE_NAME);
        } catch (IOException e) {
            System.err.println("Nao foi possivel escrever o arquivo: " + e.getMessage());
        }
    }
}