# Benchmark de Busca e Ordenacao em Java

Projeto academico em Java para demonstrar algoritmos de ordenacao e comparar o desempenho de busca sequencial e busca paralela com diferentes quantidades de threads.

O projeto tambem gera um dashboard HTML com graficos a partir dos resultados do benchmark.

## Arquivos do projeto

- `SortingAlgorithms.java`: demonstra Bubble Sort, Insertion Sort, Merge Sort e Quick Sort.
- `SearchBenchmark.java`: executa a busca sequencial e a busca paralela, gerando os tempos em CSV.
- `DashboardExporter.java`: le o CSV gerado e cria o dashboard em HTML.
- `.gitignore`: evita subir arquivos compilados, resultados gerados e possiveis arquivos sensiveis.

## Arquivos gerados

Estes arquivos sao criados ao rodar o projeto e nao precisam ser enviados para o GitHub:

- `*.class`
- `benchmark_results.csv`
- `dashboard.html`

## Pre-requisitos

Instale o JDK. O projeto foi testado com Java 21, mas tambem deve funcionar em versoes recentes do Java.

Para verificar:

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

Para executar o benchmark de busca:

```bash
java SearchBenchmark
```

Esse comando gera o arquivo `benchmark_results.csv`.

Para gerar o dashboard:

```bash
java DashboardExporter
```

Esse comando gera o arquivo `dashboard.html` e tenta abrir automaticamente o dashboard no navegador.

## Fluxo completo

```bash
javac *.java
java SortingAlgorithms
java SearchBenchmark
java DashboardExporter
```

## Observacao sobre desempenho

O benchmark usa arrays grandes, incluindo 10 milhoes, 50 milhoes e 100 milhoes de elementos. Dependendo do computador, a execucao pode demorar e consumir bastante memoria.

## GitHub

O repositorio deve versionar principalmente os arquivos `.java`, o `README.md` e o `.gitignore`. Os arquivos compilados e resultados gerados ficam fora do versionamento por causa do `.gitignore`.
