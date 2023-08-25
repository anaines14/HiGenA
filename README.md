# HiGenA
Data-Driven Hint Generation for Alloy using Historial Student Submissions

## Requirements
- Java 11
- Maven
- Python 3 and respective packages
- [Neo4j Enterprise](https://neo4j.com/download/) and the following plugins:
    - APOC 
    - Graph Data Science Library

## Build Higena
```bash
# Build Higena
$ cd higena
$ mvn clean package
# Rename the shaded jar
$ mv lib/higena/higena/1.0.0/higena-1.0.0-shaded.jar ../lib/higena/higena/1.0.0/higena-1.0.0.jar

```

## Add data
To add a new challenge, drop the .als file in the `data/datasets/challenges/` folder. The file name should be the same as the challenge ID. For example, if the challenge name is `challenge1`, the file name should be `challenge1.als`. This file does not contain secrets.

To add the dataset of student submissions for a challenge, drop the .json file in the `data/datasets/submissions/` folder. The file name should be the same as the challenge ID. For example, if the challenge name is `challenge1`, the file name should be `challenge1.json`. 
The .json file should be an array of objects, where each object represents a submission. The object should have the following fields: 
- `_id`: the submission ID
- `time`: the timestamp of its creation
- `derivationOf`: the parent entry 
- `original`: the first ancestor with secrets
- `code`: the complete code of the model 
- `sat`: command’s result. or -1 for errors [only for executions]
- `cmd_i`: the index of the executed command [only for executions]
- `cmd_n`: the name of the executed command [only for successful executions] 
- `cmd_c`: whether the command was a check [only for successful executions]
- `msg`: the error or warning message [if any] 
- `theme`: the visualisation theme [only for sharing entries]

If you prefer, you can use the dataset available at [Zenodo](https://zenodo.org/record/8123547).

## Prepare data
HiGenA requires the data to be in a specific format. To prepare the data, run the following command:
```bash
# Prepare data
$ cd data
$ python3 prepare_data.py
```

This script will create 4 folders:
- `all`: contains all the submissions
- `no_canon`: contains the submissions without canonicalization
- `only_anon`: contains the submissions with only anonymization
- `only_sort`: contains the submissions with only sorting
- `test`: contains the submissions for testing
- `train`: contains the submissions for training
- `logs`: contains the log files of the data preparation step

These folders contain one folder for each challenge. Each challenge folder contains subfolders for each public predicate in that challenge. These subfolders contain the submissions for that challenge, which contain ".csv" files, one for each predicate

## Setup neo4j

- Move the prepared data to the neo4j import/prepared_data folder.
- Enable enterprise edition.
- Increase the maximum number of databases if necessary. The default is 100. The necessary number of databases is equal to the total number of predicates in the dataset.
- Install necessary plugins:
    - apoc 
    - Graph Data Science Library
- Move the folder of submission to the neo4j import folder.
  -  Each neo4j instance contains an import folder. The default path is `/var/lib/neo4j/import/prepared_data`. If you want to create the graphs with all submissions you can move the contents of the `all` folder to the import folder. It should look like:
    ```
    import
    ├── prepared_data
    │   ├── challenge1
    │   │   ├── predicate1.csv
    │   │   ├── predicate2.csv
    │   │   ├── ...
    │   ├── challenge2
    │   │   ├── predicate1.csv
    │   │   ├── predicate2.csv
    │   │   ├── ...
    │   ├── ...
    ```
- Start the neo4j instance.

## Setup HiGenA
- HiGenA requires the neo4j instance to be running and some environment variables to be set. You can create a `.env` file in the `higena` folder of the project with the following variables:
-  URI_NEO4J: the URI of the neo4j instance, e.g. bolt://localhost:7687
-  USENAME_NEO4J: the username of the neo4j instance, e.g. neo4j
-  PASSWORD_NEO4J: the password of the neo4j instance, e.g. 1234 


## Setup Databases
- Before starting making hints for a challenge you have to create a database for that challenge. To do so, run the following command:
```bash
$ cd lib/higena/higena/1.0.0/
$ java -jar higena-1.0.0.jar $challenge $predicate
```

An alternative is to use the API.
```java 
import org.higena.graph.Graph;
// ...
Graph graph = new Graph(challenge, predicate);
graph.setup();
```

## Generate hints
To request an hint for a submission use the API.
```java
Graph graph = new Graph(challenge, predicate);
graph.getHint(expression, HintGenType.TED);
```

You can also send the code of the model if you use auxiliary predicates in 
your expression.
```java
Graph graph = new Graph(challenge, predicate);
graph.getHint(expression, code, HintGenType.TED);
```

## Structure of the project
- `data`: contains the data related files.
  - `datesets`: contains the datasets of challenges and submissions.
    - `challenges`: contains the challenges in .als format (without secrets).
    - `prepared`: contains the prepared data.
        - `all`: contains all the submissions
        - `no_canon`: contains the submissions without canonicalization
        - `only_anon`: contains the submissions with only anonymization
        - `only_sort`: contains the submissions with only sorting
        - `test`: contains the submissions for testing
        - `train`: contains the submissions for training
    - `evaluation`: contains data used for evaluation. 
    - `logs`: contains the log files of the data preparation step.
    - data_analysis.ipynb: script to analyse the data for evaluation purposes.
    - data_preparation.ipynb: script to prepare the data for HiGenA.

- `lib`: contains the libraries used by HiGenA.
- `higena`: contains the source code of HiGenA (maven project).