#!/bin/bash

#rm input-file-* output-file-* sorted-output-*
ant
for nodes in {1..5}
do
	echo "Trial " $((nodes))
	for i in {1..20}
	do
		python Project_2_GenerateInput.py 10
		java -jar wfong_p2.jar 10 $((10 * i))
		#python Grade.py
	done
done
