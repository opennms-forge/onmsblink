#!/bin/bash
java -Djava.library.path=blink1 -cp blink1/blink1.jar:target/onmsblink-1.0-SNAPSHOT-jar-with-dependencies.jar org.opennms.onmsblink.OnmsBlink $@