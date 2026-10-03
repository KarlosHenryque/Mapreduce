# Hadoop MapReduce - Netflix

Projeto acadêmico para análise do dataset da Netflix utilizando **Java, Hadoop MapReduce e Docker**.

O projeto realiza:

- Normalização das descrições;
- Remoção de stopwords;
- Identificação da descrição com maior número de palavras;
- Identificação da descrição com menor número de palavras;
- Top 5 palavras mais frequentes;
- Top 5 palavras menos frequentes;
- Total de palavras processadas.

## Estrutura

```text
trabalho-netflix/
├── dataset/
│   └── netflix_titles.csv
├── projeto/
│   ├── pom.xml
│   └── src/
├── docker-compose.yml
└── hadoop.env
```

## Pré-requisitos

- Docker Desktop
- Java
- Maven

Não é necessário instalar o Hadoop diretamente no Windows. O ambiente Hadoop é executado pelos containers Docker.

## Como executar

Abra o PowerShell na pasta raiz do projeto.

### 1. Iniciar o Hadoop

```powershell
docker compose up -d
```

### 2. Verificar os containers

```powershell
docker ps
```

Confirme que os containers do Hadoop estão em execução, principalmente o `netflix-namenode`.

### 3. Compilar o projeto

```powershell
mvn -f .\projeto\pom.xml clean package
```

O JAR será gerado em:

```text
projeto/target/netflix-mapreduce-1.0.jar
```

### 4. Executar o MapReduce

Execute:

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -mkdir -p /input; hdfs dfs -put -f /dataset/netflix_titles.csv /input/; hdfs dfs -rm -r -f /output; hadoop jar /projeto/target/netflix-mapreduce-1.0.jar /input /output; hdfs dfs -cat /output/part-r-00000"
```

Esse comando:

1. Configura o Hadoop no `PATH`;
2. Cria `/input` no HDFS;
3. Envia `netflix_titles.csv` para o HDFS;
4. Remove uma execução anterior em `/output`;
5. Executa o MapReduce;
6. Exibe o resultado no terminal.

## Visualizar o resultado novamente

Não é necessário executar todo o processamento novamente. Use:

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -cat /output/part-r-00000"
```

## Execução resumida

Para executar o projeto do zero:

```powershell
docker compose up -d

docker ps

mvn -f .\projeto\pom.xml clean package

docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -mkdir -p /input; hdfs dfs -put -f /dataset/netflix_titles.csv /input/; hdfs dfs -rm -r -f /output; hadoop jar /projeto/target/netflix-mapreduce-1.0.jar /input /output; hdfs dfs -cat /output/part-r-00000"
```

Para apenas consultar o último resultado:

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -cat /output/part-r-00000"
```

## Parar o ambiente

```powershell
docker compose down
```