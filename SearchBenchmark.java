/**
 * Atalho de compatibilidade que delega para {@link SortingBenchmark}.
 *
 * <p>O nome "SearchBenchmark" vem do enunciado original do trabalho, que descrevia
 * a atividade como "analise de algoritmos de busca". Na pratica o projeto implementa
 * algoritmos de <strong>ordenacao</strong>, portanto a logica real esta em
 * {@link SortingBenchmark}. Este arquivo e mantido para nao quebrar chamadas que
 * ainda referenciem o nome antigo.
 *
 * <p>Para executar o benchmark, prefira:
 * <pre>java SortingBenchmark</pre>
 */
public class SearchBenchmark {
    public static void main(String[] args) {
        SortingBenchmark.main(args);
    }
}