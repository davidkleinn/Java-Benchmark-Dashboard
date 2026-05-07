# Benchmark de Algoritmos Concorrentes em Java

Este repositório contém a implementação e a análise de desempenho de algoritmos clássicos de ordenação e busca, comparando suas versões sequenciais (single-thread) e paralelas (multi-thread) utilizando a linguagem Java.

Este projeto foi desenvolvido como trabalho acadêmico para a disciplina de Programação Concorrente/Paralela.

## 🎯 Objetivo

O objetivo principal é investigar o comportamento dos algoritmos sob diferentes configurações de processamento, ajustando o número de núcleos (threads) disponíveis e o tamanho dos conjuntos de dados. O projeto gera dados analíticos em formato CSV para calcular métricas de *Speedup* e demonstrar os limites da aceleração paralela (Lei de Amdahl).

## 📂 Estrutura do Repositório

* `Ordenacao.java`: Implementação dos algoritmos de ordenação Bubble Sort, Insertion Sort, Merge Sort e Quick Sort.
* `BenchmarkBusca.java`: *Framework* de testes que executa a Busca Linear sequencial e a Busca Paralela (utilizando `ExecutorService`), variando o tamanho do array e o número de threads.
* `resultados_benchmark.csv`: Arquivo gerado automaticamente pela execução do benchmark, contendo os tempos de execução de cada amostra.
* `Relatorio_Final.pdf`: (Adicione o seu PDF do relatório aqui) Apresentação dos resultados, gráficos e análises estatísticas do desempenho.

## 🚀 Como Executar

### Pré-requisitos
Certifique-se de ter o **Java Development Kit (JDK)** instalado na sua máquina (versão 8 ou superior).

Para verificar a instalação, abra o terminal e digite:
```bash
java -version
javac -version