#!/bin/bash

for f in $(find . -name "*.conf"); do
	java -jar /usr/local/opt/proguard/libexec/proguard.jar @$f
done