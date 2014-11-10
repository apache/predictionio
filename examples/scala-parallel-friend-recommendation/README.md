Parallel SimRank Algorithm
========================================================================

Parallel algorithm for calculating SimRank, using GraphX

Prerequisite
------------

GraphX

High Level Description
----------------------

Reference to paper : http://www-cs-students.stanford.edu/~glenj/simrank.pdf

### Data Source

Data source parameter takes path to edge list, which is used to specify the
graph.

### Example query 

curl -H "Content-Type: application/json" -d '[0,2]' http://localhost:8000/queries.json

This queries the SimRank score between nodes with ids 0 and 2,
