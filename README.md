# Projeto Big Data - Análise de dados da Netflix com Hadoop e MapReduce

## Nome da apresentação
Análise de dados da Netflix usando Hadoop, Java e MapReduce

## Introdução

Este projeto foi desenvolvido para mostrar na prática como funciona o processamento de grandes volumes de dados usando o Hadoop. A ideia principal é analisar as descrições dos títulos da Netflix, limpar o texto, contar palavras e descobrir padrões importantes.

Ao invés de processar o arquivo de forma simples em um único computador, o Hadoop divide a tarefa em partes menores, processa essas partes em paralelo e depois reúne os resultados em uma resposta final.

---

## Objetivo do projeto

O projeto tem como objetivo:

- ler o dataset da Netflix;
- analisar as descrições dos títulos;
- remover palavras sem valor para a análise, como artigos e preposições;
- contar a frequência das palavras;
- identificar a descrição com maior número de palavras;
- identificar a descrição com menor número de palavras;
- mostrar as palavras mais frequentes e menos frequentes;
- apresentar o total de palavras processadas.

---

## O que é Big Data?

Big Data é o estudo e o uso de grandes quantidades de dados que não podem ser processados facilmente com ferramentas tradicionais. Em muitos casos, os dados são muito grandes, muito variados e chegam em alta velocidade.

Nesse projeto, o dataset da Netflix possui várias linhas e diversos campos, então a melhor forma de analisar esses dados é usar o Hadoop, que distribui a carga entre vários nós e processa as informações de maneira mais eficiente.

---

## O que está sendo analisado?

O arquivo principal do projeto é o CSV da Netflix, que contém informações como:

- código do título;
- tipo (filme ou série);
- nome do título;
- país;
- ano de lançamento;
- classificação;
- duração;
- descrição do conteúdo.

A parte mais importante neste projeto é a coluna de descrição, porque é nela que a análise textual acontece.

---

## Como o projeto funciona?

### 1. Leitura do arquivo
O programa lê o CSV da Netflix em formato texto. Cada linha representa um título com suas informações.

### 2. Limpeza do texto
As descrições são tratadas para remover:

- acentos;
- pontuação;
- letras maiúsculas;
- caracteres especiais;
- palavras irrelevantes.

Isso ajuda a padronizar o texto para a contagem de palavras.

### 3. Remoção de stopwords
Stopwords são palavras muito comuns que geralmente não ajudam a análise, como:

- the
- a
- and
- of
- in
- to

Essas palavras são ignoradas para evitar que a contagem fique prejudicada por termos sem significado.

### 4. Contagem de palavras
Cada palavra encontrada na descrição é contada. Por exemplo:

- descrição: "A family lives in a big city"
- palavras relevantes: family, lives, big, city

### 5. Agrupamento dos resultados
Depois que cada parte do arquivo foi processada, o sistema reúne todos os resultados e calcula:

- qual título tem a maior descrição;
- qual título tem a menor descrição;
- quais palavras aparecem mais vezes;
- quais palavras aparecem menos vezes;
- total de palavras processadas.

---

## O que é Hadoop?

Hadoop é uma plataforma de processamento distribuído. Ele foi criado para trabalhar com grandes volumes de dados, usando clusters de computadores para armazenar e processar informações.

No projeto, o Hadoop é usado para:

- armazenar os dados no HDFS;
- distribuir o processamento;
- executar o job do MapReduce;
- gerar a saída final com os resultados da análise.

---

## O que é MapReduce?

MapReduce é um modelo de programação usado para processar grandes dados em ambiente distribuído.

Ele funciona em duas fases:

### Fase Map
A fase Map lê cada linha do arquivo e transforma os dados em pares chave-valor.

Exemplo:

- palavra -> 1

Assim, cada vez que a palavra aparece, ela é contabilizada.

### Fase Reduce
A fase Reduce recebe todos os pares gerados pelo Map e junta as informações.

Exemplo:

- love -> 50
- family -> 42
- drama -> 38

Essa fase é responsável por somar tudo e entregar o resultado final.

---

## Estrutura do projeto

```text
trabalho-netflix/
├── dataset/
│   └── netflix_titles.csv
├── projeto/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── br/
│           │       └── edu/
│           │           └── netflix/
│           │               ├── CsvInputFormat.java
│           │               ├── CsvRecordReader.java
│           │               ├── NetflixDriver.java
│           │               ├── NetflixMapper.java
│           │               └── NetflixReducer.java
│           └── resources/
│               └── stopwords.txt
├── docker-compose.yml
├── hadoop.env
├── README.md
└── .gitignore
```

---

## Arquivos principais e sua função

### docker-compose.yml
Esse arquivo cria e inicia os containers do Hadoop usando Docker. Ele configura o ambiente necessário para rodar o projeto.

### hadoop.env
Arquivo de configuração com variáveis do ambiente Hadoop.

### pom.xml
Arquivo do Maven que define as dependências do projeto, como Java e Hadoop, além da configuração para gerar o arquivo executável.

### NetflixDriver.java
É a classe principal que inicia o job do Hadoop. Ela define:

- a classe Mapper;
- a classe Reducer;
- o tipo de entrada do arquivo;
- o caminho de entrada e saída.

### NetflixMapper.java
Essa classe faz a leitura e a preparação dos dados. Ela:

- verifica cada linha do CSV;
- extrai a descrição do título;
- limpa o texto;
- remove as stopwords;
- conta as palavras;
- envia os dados para o Reduce.

### NetflixReducer.java
Essa classe recebe os dados processados pelo Mapper e faz as contas finais. Ela organiza tudo para mostrar os resultados finais.

### stopwords.txt
Arquivo que contém todas as palavras ignoradas na análise textual.

---

## Exemplo de saída

Ao final da execução, o programa mostra algo parecido com isso:

```text
--- RESULTADO ---
TITULO_MAIOR_DESCRICAO
Título: X
Palavras: 120

TITULO_MENOR_DESCRICAO
Título: Y
Palavras: 5

TOP_5_MAIS_FREQUENTES
1. family = 142
2. love = 129
3. life = 118
4. world = 110
5. drama = 98

TOP_5_MENOS_FREQUENTES
1. adventure = 1
2. mystery = 1
3. comedy = 1
4. romance = 1
5. action = 1

TOTAL_PALAVRAS
45678
```

---

## Como executar o projeto

### 1. Subir o ambiente do Hadoop

```powershell
docker compose up -d
```

### 2. Compilar o projeto Java

```powershell
mvn -f .\projeto\pom.xml clean package
```

### 3. Executar o job no Hadoop

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -mkdir -p /input; hdfs dfs -put -f /dataset/netflix_titles.csv /input/; hdfs dfs -rm -r -f /output; hadoop jar /projeto/target/netflix-mapreduce-1.0.jar /input /output; hdfs dfs -cat /output/part-r-00000"
```

Esse comando faz as seguintes etapas:

1. cria a pasta de entrada no HDFS;
2. envia o CSV para o ambiente Hadoop;
3. remove a saída antiga;
4. executa o job de análise;
5. mostra o resultado no terminal.

### 4. Consultar o resultado novamente

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -cat /output/part-r-00000"
```

---

## Conclusão

Este projeto foi criado para demonstrar, de forma prática, como o Hadoop e o MapReduce podem ser usados para processar dados em grande escala. Através da análise de descrições da Netflix, o sistema mostra que é possível transformar texto bruto em informações úteis, como contagem de palavras, títulos mais longos, palavras mais frequentes e total de palavras processadas.

Em outras palavras, o projeto demonstra o funcionamento do Big Data em um contexto real, simples e visual.

---

## Resumo para apresentação oral

> Este projeto analisa os dados da Netflix usando Hadoop e MapReduce. Primeiro, o sistema lê as descrições dos títulos, limpa o texto, remove palavras pouco relevantes e conta as palavras. Depois, o Hadoop reúne esses resultados para identificar a descrição com mais palavras, a com menos palavras, as palavras mais frequentes e o total geral de palavras processadas. Em resumo, o projeto mostra na prática como o processamento distribuído funciona e como grandes volumes de dados podem ser transformados em informações úteis.


---

## Resumo simples para apresentação

> Este projeto usa Hadoop para analisar o dataset da Netflix. A primeira parte separa as palavras das descrições, remove termos comuns e conta cada ocorrência. A segunda parte reúne essas contagens e mostra qual título tem mais palavras, qual tem menos palavras, quais palavras aparecem mais e quantas palavras foram processadas no total. Em outras palavras, o projeto transforma texto em informações úteis usando processamento distribuído.

---

## Conclusão

O objetivo do trabalho não é só contar palavras. O objetivo principal é mostrar como o Big Data funciona na prática: dividir um problema grande em partes menores, processar essas partes em paralelo e depois reuní-las para formar uma resposta final.

É um exemplo muito bom de como tecnologias como Hadoop, MapReduce e Java podem ser usadas para analisar grandes quantidades de dados reais.
