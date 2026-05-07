# Benchmark de Algoritmos de Ordenacao em Java

Projeto academico em Java para implementar algoritmos de ordenacao e comparar o desempenho entre versoes seriais e paralelas, variando o tamanho da entrada e a quantidade de threads.

O benchmark gera um CSV com os tempos medidos e o projeto tambem cria um dashboard HTML dinamico com graficos desses resultados.

## Algoritmos

Versoes seriais:

- Bubble Sort
- Insertion Sort
- Merge Sort
- Quick Sort

Versoes paralelas:

- Merge Sort com `ForkJoinPool`
- Quick Sort com `ForkJoinPool`

Bubble Sort e Insertion Sort ficam apenas na versao serial porque nao sao boas escolhas praticas para paralelizacao. Merge Sort e Quick Sort foram escolhidos para as versoes paralelas por seguirem divisao e conquista.

## Arquivos

- `SortingAlgorithms.java`: implementa os algoritmos seriais e paralelos.
- `SortingBenchmark.java`: executa o benchmark de ordenacao e gera `benchmark_results.csv`.
- `SearchBenchmark.java`: atalho legado que chama `SortingBenchmark`.
- `DashboardExporter.java`: le o CSV e gera `dashboard.html`.
- `.gitignore`: ignora arquivos compilados e arquivos gerados pelo benchmark.

## Pre-requisitos

Use um JDK recente. O projeto foi testado com Java 21.

```bash
java -version
javac -version
```

## Como Compilar

Na pasta do projeto:

```bash
javac *.java
```

## Como Rodar

Para demonstrar os algoritmos com um array pequeno:

```bash
java SortingAlgorithms
```

Para executar o benchmark com a configuracao padrao:

```bash
java SortingBenchmark
```

Padrao atual:

- tamanhos: `1.000`, `5.000` e `10.000` elementos
- amostras: `5` por configuracao
- threads paralelas: `2`, `4`, `8` e `16`
- saida: `benchmark_results.csv`

Para informar tamanhos e numero de amostras:

```bash
java SortingBenchmark "10000,50000,100000" 5
```

Para rodar somente alguns algoritmos, informe o filtro no terceiro argumento:

```bash
java SortingBenchmark "100000,500000,1000000" 3 "merge,quick"
```

Filtros aceitos funcionam por nome parcial, como `bubble`, `insertion`, `merge`, `quick` ou `all`.

## Dashboard

Depois de gerar o CSV:

```bash
java DashboardExporter
```

Esse comando cria `dashboard.html` e tenta abrir o arquivo no navegador.

Para gerar o HTML sem abrir automaticamente:

```bash
java DashboardExporter benchmark_results.csv dashboard.html --no-open
```

O dashboard permite filtrar por algoritmo e tamanho de entrada, mostrando tempo medio, speedup e variacao entre amostras.

## Fluxo Completo

```bash
javac *.java
java SortingAlgorithms
java SortingBenchmark
java DashboardExporter
```

## Formato do CSV

O arquivo `benchmark_results.csv` usa as colunas:

- `Algorithm`: algoritmo testado
- `DataSize`: tamanho do array
- `ExecutionMode`: `Sequential` ou `Parallel`
- `Threads`: quantidade de threads usada
- `Sample`: numero da amostra
- `TimeMs`: tempo em milissegundos
- `Sorted`: confirma se a saida ficou ordenada

## Observacoes de Desempenho

Bubble Sort e Insertion Sort podem ficar muito lentos em entradas grandes. Para testes com centenas de milhares ou milhoes de elementos, use preferencialmente:

```bash
java SortingBenchmark "100000,500000,1000000" 3 "merge,quick"
```

Os arquivos `*.class`, `benchmark_results.csv` e `dashboard.html` sao gerados localmente e ficam fora do versionamento.
