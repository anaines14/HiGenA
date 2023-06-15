# HiGenA
Data-Driven Hint Generation for Alloy using Historial Student Submissions

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

## Prepare data
```bash
# Prepare data
$ cd data
$ python3 prepare_data.py
```

This script will create 4 folders:
- `all`: contains all the submissions
- `test`: contains the submissions for testing
- `train`: contains the submissions for training
- `logs`: contains the log files of the data preparation step

The folders that contain submissions contain subfolders, one for each challenge. Each subfolder contains the submissions for that challenge which contains 
".csv" files, one for each predicate

## Setup neo4j

- Move the prepared data to the neo4j import/prepared_data folder.
- Enable enterprise edition
- Increase the maximum number of databases if necessary. The default is 100. The necessary number of databases is equal to the total number of predicates in the dataset.
- Install necessary plugins:
    - apoc 
    - Graph Data Science Library
- Start neo4j

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

## Make hints
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