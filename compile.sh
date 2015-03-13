#!/usr/bin/env bash

javac -classpath ./commons-lang3-3.1.jar EmpPriorSearch.java BranchLengthProgress.java TreeBaseConnectionMT.java
jar -cvmf manifest.txt EmpPrior-search.jar  *.class commons-lang3-3.1.jar

