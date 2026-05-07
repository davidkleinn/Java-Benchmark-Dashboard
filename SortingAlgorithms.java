import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class SortingAlgorithms {
    private static final int PARALLEL_THRESHOLD = 16_384;

    // ==========================================
    // 1. Bubble Sort
    // ==========================================
    public static void bubbleSort(int[] array) {
        int length = array.length;
        boolean swapped;
        for (int i = 0; i < length - 1; i++) {
            swapped = false;
            for (int j = 0; j < length - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    // ==========================================
    // 2. Insertion Sort
    // ==========================================
    public static void insertionSort(int[] array) {
        int length = array.length;
        for (int i = 1; i < length; i++) {
            int key = array[i];
            int j = i - 1;

            while (j >= 0 && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }

    // ==========================================
    // 3. Merge Sort
    // ==========================================
    public static void mergeSort(int[] array, int left, int right) {
        if (left < right) {
            int middle = left + (right - left) / 2;

            mergeSort(array, left, middle);
            mergeSort(array, middle + 1, right);
            merge(array, left, middle, right);
        }
    }

    public static void parallelMergeSort(int[] array, int threadCount) {
        if (array.length < 2) return;
        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            pool.invoke(new MergeSortTask(array, 0, array.length - 1));
        } finally {
            pool.shutdown();
        }
        pool.close();
    }

    private static class MergeSortTask extends RecursiveAction {
        private final int[] array;
        private final int left;
        private final int right;

        MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left <= PARALLEL_THRESHOLD) {
                mergeSort(array, left, right);
                return;
            }

            int middle = left + (right - left) / 2;
            invokeAll(new MergeSortTask(array, left, middle),
                    new MergeSortTask(array, middle + 1, right));
            merge(array, left, middle, right);
        }
    }

    private static void merge(int[] array, int left, int middle, int right) {
        int leftSize = middle - left + 1;
        int rightSize = right - middle;

        int[] leftArray = new int[leftSize];
        int[] rightArray = new int[rightSize];

        for (int i = 0; i < leftSize; i++) leftArray[i] = array[left + i];
        for (int j = 0; j < rightSize; j++) rightArray[j] = array[middle + 1 + j];

        int i = 0;
        int j = 0;
        int k = left;
        while (i < leftSize && j < rightSize) {
            if (leftArray[i] <= rightArray[j]) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            k++;
        }

        while (i < leftSize) {
            array[k] = leftArray[i];
            i++;
            k++;
        }
        while (j < rightSize) {
            array[k] = rightArray[j];
            j++;
            k++;
        }
    }

    // ==========================================
    // 4. Quick Sort
    // ==========================================
    public static void quickSort(int[] array, int start, int end) {
        if (start < end) {
            int pivotIndex = partition(array, start, end);

            quickSort(array, start, pivotIndex - 1);
            quickSort(array, pivotIndex + 1, end);
        }
    }

    public static void parallelQuickSort(int[] array, int threadCount) {
        if (array.length < 2) return;
        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            pool.invoke(new QuickSortTask(array, 0, array.length - 1));
        } finally {
            pool.shutdown();
        }
        pool.close();
    }

    private static class QuickSortTask extends RecursiveAction {
        private final int[] array;
        private final int start;
        private final int end;

        QuickSortTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= PARALLEL_THRESHOLD) {
                quickSort(array, start, end);
                return;
            }

            int pivotIndex = partition(array, start, end);
            invokeAll(new QuickSortTask(array, start, pivotIndex - 1),
                    new QuickSortTask(array, pivotIndex + 1, end));
        }
    }

    private static int partition(int[] array, int start, int end) {
        int pivot = array[end];
        int i = start - 1;

        for (int j = start; j < end; j++) {
            if (array[j] <= pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }

        int temp = array[i + 1];
        array[i + 1] = array[end];
        array[end] = temp;

        return i + 1;
    }

    // ==========================================
    // Demo Runner
    // ==========================================
    public static void main(String[] args) {
        int[] originalArray = {64, 34, 25, 12, 22, 11, 90, 7, 50};

        System.out.println("Original array: " + Arrays.toString(originalArray));
        System.out.println("--------------------------------------------------");

        int[] bubbleSorted = originalArray.clone();
        bubbleSort(bubbleSorted);
        System.out.println("Bubble Sort:    " + Arrays.toString(bubbleSorted));

        int[] insertionSorted = originalArray.clone();
        insertionSort(insertionSorted);
        System.out.println("Insertion Sort: " + Arrays.toString(insertionSorted));

        int[] mergeSorted = originalArray.clone();
        mergeSort(mergeSorted, 0, mergeSorted.length - 1);
        System.out.println("Merge Sort:     " + Arrays.toString(mergeSorted));

        int[] quickSorted = originalArray.clone();
        quickSort(quickSorted, 0, quickSorted.length - 1);
        System.out.println("Quick Sort:     " + Arrays.toString(quickSorted));

        int[] parallelMergeSorted = originalArray.clone();
        parallelMergeSort(parallelMergeSorted, 4);
        System.out.println("Parallel Merge: " + Arrays.toString(parallelMergeSorted));

        int[] parallelQuickSorted = originalArray.clone();
        parallelQuickSort(parallelQuickSorted, 4);
        System.out.println("Parallel Quick: " + Arrays.toString(parallelQuickSorted));
    }
}
