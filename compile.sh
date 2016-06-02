#!/usr/bin/env bash

javac EmpPriorSearch.java BranchLengthProgress.java TreeBaseConnectionMT.java
jar -cvmf manifest.txt EmpPrior-search.jar  *.class

