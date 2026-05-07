# Benchmark de Algoritmos de Ordenacao em Java

Projeto academico em Java para implementar algoritmos de ordenacao e comparar o desempenho das versoes seriais e paralelas com diferentes tamanhos de entrada e quantidades de threads.

O projeto gera um arquivo CSV com os tempos coletados e um dashboard HTML dinamico com graficos a partir desses resultados.

## Algoritmos implementados

- Bubble Sort serial
- Insertion Sort serial
- Merge Sort serial
- Quick Sort serial
- Merge Sort paralelo com `ForkJoinPool`
- Quick Sort paralelo com `ForkJoinPool`

Bubble Sort e Insertion Sort foram mantidos como versoes seriais porque sao algoritmos simples, com baixo ganho pratico de paralelizacao. As versoes paralelas foram aplicadas em Merge Sort e Quick Sort, que seguem a estrategia de divisao e conquista.

## Arquivos do projeto

- `SortingAlgorithms.java`: implementa os algoritmos de ordenacao seriais e paralelos.
- `SortingBenchmark.java`: executa o benchmark de ordenacao e gera o CSV.
- `SearchBenchmark.java`: atalho de compatibilidade que chama `SortingBenchmark`.
- `DashboardExporter.java`: le o CSV gerado e cria o dashboard HTML.

## Arquivos gerados

Estes arquivos sao criados ao rodar o projeto mas nao precisaram ser enviados para o GitHub:

- `*.class`
- `benchmark_results.csv`
- `dashboard.html`

## Pre-requisitos

Instale o JDK. O projeto foi testado com Java 21, mas tambem deve funcionar em versoes recentes do Java.

```bash
java -version
javac -version
```

## Como compilar

Na pasta do projeto, execute:

```bash
javac *.java
```

## Como rodar

Para executar a demonstracao dos algoritmos de ordenacao:

```bash
java SortingAlgorithms
```

Para executar o benchmark de ordenacao:

```bash
java SortingBenchmark
```

Esse comando gera o arquivo `benchmark_results.csv`. Por padrao, ele usa 1.000, 5.000 e 10.000 elementos, com 5 amostras por configuracao.

Tambem e possivel informar os tamanhos e a quantidade de amostras:

```bash
java SortingBenchmark "10000,50000,100000" 5
```

Para testes maiores, voce pode filtrar os algoritmos e evitar Bubble/Insertion:

```bash
java SortingBenchmark "100000,500000,1000000" 3 "merge,quick"
```

Para gerar o dashboard:

```bash
java DashboardExporter
```

Esse comando gera o arquivo `dashboard.html` e tenta abrir automaticamente o dashboard no navegador.

## Fluxo completo

```bash
javac *.java
java SortingAlgorithms
java SortingBenchmark
java DashboardExporter
```

## Observacao sobre desempenho

Ordenar arrays muito grandes com Bubble Sort ou Insertion Sort pode demorar muito. Para testes com milhoes de elementos, e recomendado reduzir os algoritmos testados ou focar em Merge Sort e Quick Sort.

O benchmark registra as colunas `Algorithm`, `DataSize`, `ExecutionMode`, `Threads`, `Sample`, `TimeMs` e `Sorted`. A coluna `Sorted` confirma se a ordenacao produziu uma saida correta.
