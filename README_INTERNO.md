# Guia interno do projeto Netflix MapReduce

Este documento serve como material de estudo e roteiro para explicar o projeto ao professor. Ele descreve o comportamento implementado no código, inclusive o que entra e o que fica fora das contagens.

## Objetivo

Analisar as descrições do arquivo `netflix_titles.csv` usando Java e Hadoop MapReduce. O programa calcula:

- O título com a descrição que tem mais palavras consideradas;
- O título com a descrição que tem menos palavras consideradas;
- As cinco palavras mais frequentes;
- As cinco palavras menos frequentes;
- O total de palavras consideradas no conjunto de descrições.

Uma palavra é considerada depois da normalização e da remoção das stopwords. Portanto, esses números não representam a contagem bruta de palavras do CSV.

## Tecnologias e organização

- **Java 8:** implementação do Mapper, Reducer e Driver;
- **Apache Hadoop MapReduce:** processamento distribuído em etapas Map e Reduce;
- **HDFS:** armazenamento do arquivo de entrada e do resultado;
- **Docker Compose:** execução local dos serviços Hadoop em containers;
- **Maven:** compilação e criação do JAR executável.

Arquivos principais:

| Arquivo | Responsabilidade |
| --- | --- |
| `projeto/src/main/java/br/edu/netflix/NetflixMapper.java` | Lê cada registro, normaliza a descrição, remove stopwords e emite contagens intermediárias. |
| `projeto/src/main/java/br/edu/netflix/NetflixReducer.java` | Soma as contagens, calcula os extremos, ordena frequências e escreve o relatório. |
| `projeto/src/main/java/br/edu/netflix/NetflixDriver.java` | Configura e inicia o Job do Hadoop. |
| `projeto/src/main/resources/stopwords.txt` | Lista de palavras que não entram na análise. |
| `projeto/pom.xml` | Dependências e configuração de compilação do Java/JAR. |
| `docker-compose.yml` | Define os containers do Hadoop e os diretórios compartilhados. |
| `dataset/netflix_titles.csv` | Dataset usado como entrada. |

## Fluxo do processamento

```text
CSV no diretório dataset/
        |
        v
Container Hadoop lê o arquivo do HDFS
        |
        v
Mapper identifica colunas e processa cada título
        |
        v
Shuffle/Sort agrupa os valores pela chave
        |
        v
Reducer soma e calcula as estatísticas
        |
        v
Relatório em /output/part-r-00000 no HDFS
```

### 1. Driver

O `NetflixDriver` recebe dois argumentos: o caminho de entrada e o caminho de saída no HDFS. Ele registra as classes Mapper e Reducer, configura os tipos intermediários e inicia o Job chamado `netflix-analysis`.

O Job usa **um Reducer** (`setNumReduceTasks(1)`). Isso é importante porque o projeto precisa comparar todas as descrições e montar um ranking geral único. Com mais de um Reducer, cada um poderia calcular apenas um resultado parcial.

### 2. Mapper: leitura e preparação

Na inicialização, o Mapper carrega `stopwords.txt` do classpath do JAR. Na primeira linha do CSV, ele procura os nomes das colunas `title` e `description`, então não depende de índices fixos para esses campos.

Para cada registro, o Mapper:

1. Separa as colunas do CSV respeitando vírgulas dentro de aspas e aspas duplicadas;
2. Obtém o título e a descrição;
3. Converte o texto para minúsculas e remove acentos;
4. Remove apóstrofos e substitui pontuação por espaços;
5. Divide o texto em palavras e descarta palavras vazias e stopwords;
6. Emite uma ocorrência por palavra restante e as contagens da descrição e do conjunto.

As chaves emitidas pelo Mapper têm estes formatos:

| Chave intermediária | Valor | Significado |
| --- | --- | --- |
| `WORD\|palavra` | `1` | Uma ocorrência daquela palavra. |
| `DESCRIPTION\|título` | quantidade | Quantidade de palavras consideradas naquela descrição. |
| `TOTAL` | quantidade | Quantidade de palavras consideradas naquele registro, para somar o total. |

Exemplo simplificado: se uma descrição, depois da limpeza, resultar em `good movie`, o Mapper emite `WORD|good -> 1`, `WORD|movie -> 1`, `DESCRIPTION|Título -> 2` e `TOTAL -> 2`.

### 3. Shuffle e Sort do Hadoop

Entre o Mapper e o Reducer, o Hadoop agrupa todos os valores que possuem a mesma chave. Por exemplo, todas as ocorrências de `WORD|life` são reunidas para que o Reducer possa somá-las. Essa etapa é chamada de Shuffle and Sort e é fornecida pelo Hadoop.

### 4. Reducer: agregação e relatório

O Reducer soma os valores recebidos para cada chave:

- Para `WORD|...`, guarda a frequência de cada palavra em um mapa;
- Para `DESCRIPTION|...`, compara a quantidade da descrição com os maiores e menores valores encontrados;
- Para `TOTAL`, acumula o total geral de palavras consideradas.

No final do Job, o método `cleanup` escreve o relatório. As frequências são ordenadas pela quantidade; em caso de empate, as palavras são ordenadas alfabeticamente. O código seleciona até cinco palavras de cada ranking.

O Reducer escreve somente o texto de cada linha (`NullWritable` como chave de saída), por isso o arquivo final não recebe uma chave Hadoop repetida antes de cada linha. O relatório inclui linhas em branco para separar os blocos.

## Como executar

No PowerShell, a partir da pasta raiz do projeto:

```powershell
docker compose up -d
mvn -f .\projeto\pom.xml clean package
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -mkdir -p /input; hdfs dfs -put -f /dataset/netflix_titles.csv /input/; hdfs dfs -rm -r -f /output; hadoop jar /projeto/target/netflix-mapreduce-1.0.jar /input /output; hdfs dfs -cat /output/part-r-00000"
```

O diretório `dataset/` e o projeto `projeto/` são montados no container `netflix-namenode`, conforme o `docker-compose.yml`. O comando copia o CSV local para `/input` no HDFS, remove uma saída antiga, executa o JAR e imprime o resultado. O Hadoop exige que o caminho de saída não exista antes da execução, por isso o comando remove `/output`.

Para consultar o resultado sem processar novamente:

```powershell
docker exec -it netflix-namenode bash -lc "export PATH=/opt/hadoop-3.2.1/bin:`$PATH; hdfs dfs -cat /output/part-r-00000"
```

Para encerrar os containers:

```powershell
docker compose down
```

## Como explicar ao professor

> O projeto analisa as descrições do catálogo da Netflix com Hadoop MapReduce. O Mapper identifica as colunas do CSV, normaliza o texto, remove as stopwords e emite contagens intermediárias para cada palavra, descrição e para o total. O Hadoop agrupa essas chaves. Depois, o Reducer soma as frequências, encontra as descrições com maior e menor quantidade de palavras e monta os rankings. O resultado fica no HDFS e é exibido no terminal. Usei um Reducer para produzir estatísticas globais em um único relatório.

Se perguntarem sobre as stopwords, explique que são palavras comuns, como artigos, pronomes e preposições, listadas em `stopwords.txt`; elas são excluídas para destacar termos mais informativos. Se perguntarem o que significa “quantidade de palavras”, esclareça que são os tokens após normalização e remoção de stopwords.

## Limitações e pontos para conhecer

- O programa contabiliza palavras das descrições, não palavras dos títulos.
- Descrições vazias ou sem palavras restantes após a remoção de stopwords não são contabilizadas nem usadas na comparação de maior/menor descrição.
- A lista de stopwords é pequena e definida manualmente; os resultados dependem do conteúdo desse arquivo.
- A limpeza remove pontuação e apóstrofos e transforma acentos em letras sem acento. Não há análise linguística, lematização ou reconhecimento de expressões compostas.
- A leitura do CSV trata vírgulas e aspas dentro de uma linha, mas não implementa registros CSV que continuem em múltiplas linhas dentro de um campo entre aspas.
- Um único Reducer facilita obter um ranking global, mas limita a escalabilidade: ele precisa manter as frequências das palavras em memória e ordenar o vocabulário completo.
- A imagem Docker usa Hadoop 3.2.1, enquanto o `pom.xml` declara bibliotecas Hadoop 3.3.6. Essa diferença de versões é um detalhe do ambiente atual e deve ser considerada caso ocorram incompatibilidades ao atualizar os containers ou dependências.

## Estrutura esperada do resultado

O relatório contém o título e a quantidade de palavras das descrições extremas, os dois rankings com até cinco palavras, e o total geral. Os valores dependem do CSV utilizado e do conteúdo atual de `stopwords.txt`.