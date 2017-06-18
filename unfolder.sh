#!/bin/sh

dest=unfoldedTraining

mkdir -p $dest

for catFolder in train/*; do
    for file in $catFolder/*; do
    	category=`echo $file | cut -d '/' -f2`
        filename=`echo $file | cut -d '/' -f3`
        cp $file $dest/$category-$filename
    done
done
