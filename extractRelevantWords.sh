#!/bin/sh
rm relevantIndices.txt 
for file in output*.txt; do
	echo $file;
	cat $file | grep "*" | grep -v "Intercept" | grep -v "Signif" | cut -c1-8 | sed 's/dat//g' >> relevantIndices.txt
done