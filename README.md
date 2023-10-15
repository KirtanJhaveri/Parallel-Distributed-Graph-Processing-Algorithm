# Parallel Distributed Graph Processing Algorithm
- NetID: kjhave3@uic.edu
- Name: Kirtan Jhaveri
- youtube video: 
https://youtu.be/yNTtfDlJVaI

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Usage](#usage)
- [Input](#input)
- [Output](#output)
- [Implementation Details](#implementation-details)

## Introduction

This program utilizes a parallel distributed processing algorithm designed for handling large graphs. It focuses on computing various matching statistics such as node and edge distributions, similarities, and modifications in perturbed graphs. Additionally, the algorithm evaluates traceability links and calculates accuracy, precision, etc

## Features

- **Parallel Processing**: Utilizes Map/Reduce to process and compare graphs.
- **Matching Statistics**:The program calculates various matching statistics using the Jaccard Index, a popular similarity measure used in graph analysis. The Jaccard Index measures the similarity between two sets by dividing the size of the intersection by the size of the union of the sets. In the context of graph analysis, it is commonly used to compute similarities between nodes and edges in both the original and perturbed graphs.

- **Node Similarities**: The Jaccard Index is used to compute the similarity between nodes in the original and perturbed graphs. High Jaccard Index values indicate nodes that are similar between the graphs.
  
- **Edge Similarities**: Similar to nodes, the Jaccard Index is applied to edges to measure their similarity between the original and perturbed graphs.

- **Node and Edge Distributions**: The program analyzes the distribution of nodes and edges in both graphs. It provides insights into the overall structure of the graphs and identifies patterns in node and edge connections.

- **Modifications in Perturbed Graphs**: By comparing the Jaccard Index scores, the program detects modifications, including added, removed, or modified nodes and edges in the perturbed graphs compared to the original ones.

The Jaccard Index calculations provide a quantitative measure of the similarity between graph components, aiding in understanding the structural changes introduced in the perturbed graphs.

These computed statistics are essential for understanding the impact of perturbations on the graph structure and evaluating the effectiveness of the parallel distributed processing algorithm.

- **Metrics Calculation**: Computes accuracy (ACC), precision (VPR), and other ratios to assess algorithm performance in comparison to the goldenSet of pertubations between the graphs

## Usage

To use the program, follow these steps:

1. **Install Dependencies**: Ensure all necessary dependencies are installed (see [Dependencies](#dependencies)).
2. **Run the Program**: Execute the main program file with appropriate input parameters.
3. **Review Output**: Analyze the generated YAML/CSV file containing node and edge distributions, as well as modification likelihoods.
4. **Evaluate Metrics**: Examine the computed metrics such as ACC, VPR, and BTLR to assess the algorithm's performance.

## Input

The program takes input in the form of large graphs and their perturbed counterparts for parallel distributed processing. Input data should be formatted according to the specified graph representation.
Please provide the .ngs and the .ngs.perturbed files as input to the program in the following directory of the project:
`outputs/40_nodes.ngs`
The Main invocation file is:
`ProjectMain.scala`
The code takes in 10 arguments in the run configuration(in intellij), below are the arguments set in my run config please change them according to path on your system **(Note: This Project uses the local filesystem NOT HDFS)**:

**Using intellij run config**:

`PATH_TO\NetGameSim\outputs\40_nodes.ngs` 
`PATH_TO\NetGameSim\outputs\40_nodes.ngs.perturbed`
`PATH_TO\NetGameSim\outputs\output_nodes.txt`
`PATH_TO\CS441\NetGameSim\src\main\resources\output_nodes`
`PATH_TO\CS441\NetGameSim\outputs\output_edges.txt`
`PATH_TO\CS441\NetGameSim\src\main\resources\output_edges`
`PATH_TO\CS441\NetGameSim\src\main\resources\final`
`PATH_TO\CS441\NetGameSim\src\main\resources\final\part-00000`
`PATH_TO\CS441\NetGameSim\outputs\40_nodes.ngs.yaml`
`PATH_TO\CS441\NetGameSim\outputs`

**Using SBT**:

`SBT "run outputs\300_nodes.ngs outputs\300_nodes.ngs.perturbed outputs\output_nodes.txt src\main\resources\output_nodes outputs\output_edges.txt src\main\resources\output_edges src\main\resources\final src\main\resources\final\part-00000 outputs\40_nodes.ngs.yaml outputs"`

## Output

The program produces the following outputs as logs on the console:
- **Traceability Links**: Evaluation results, including ACC, VPR, and BTLR metrics.


## Implementation Details

The program is implemented using `Scala` and utilizes `Map/Reduce` for parallel processing and graph analysis. The graph matching algorithm is fine-tuned to detect modifications accurately. Detailed implementation information can be found in the source code.

There are 4 files in the project:

`ProjectMain.Scala`: 
- The main file of the project that is to be run, it takes in two serialized(.ngs) graph files as input and extracts nodes and edges from both the graphs.
- It then makes all the possible permutation and combinations of nodes from both the graphs and similarly for edges.
- Then writes both of them to a file which can be given as input to the Map/Reduce Jobs

`MapReduceNodes.scala:`
- This is the first Map/Reduce Job that is run to calculate similarity between all possible node combinations from original and perturbed graph.
- The SimScore is calculated using Jaccardindex based on each attrinbute of a node.

    **Note:Unique Node Ids are NOT considered when calculating SimScore. They have been eliminated by using Regex**

`MapReduceEdges.scala:`
- This is the Second Map/Reduce Job that is run to calculate similarity between all possible edge combinations from original and perturbed graph.
- The SimScore is calculated using Jaccardindex based on each attrinbute of a edge but it excludes the node properties for similarity Calculation.

    **Note:Unique Node Ids are NOT considered when calculating SimScore. They have been eliminated by using Regex**

`MapReduceCheck.scala`:
- This is the Third Map/Reduce Job that is run to combine the outputs of the previous two Map/Reduce Jobs and Decide whether a node/edge was perturbed based on a threshold.
- Threshold Values: 
`NodeRemoved,EdgeRemoved : Jaccardindex<0.3`
`NodeUnchaged,EdgeUnchanged : Jaccardindex = 1`
`NodeRemoved,EdgeRemoved : 0.3<Jaccardindex<=0.99`

`CombinedParser.scala`:

- This is the last file that will be executed, It compares the output file from `MapReduceCheck.scala` with the GoldenSet .yaml file to calculate,TLs,accuracy,precision and BTLR.
