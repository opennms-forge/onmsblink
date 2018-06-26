#!/bin/bash
cd `dirname $0`
java -Djava.library.path=blink1-java/build -cp blink1-java/build/blink1.jar:target/onmsblink-1.0-SNAPSHOT-jar-with-dependencies.jar org.opennms.onmsblink.OnmsBlink "$@"
