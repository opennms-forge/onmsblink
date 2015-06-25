# onmsblink
A small tool to indicate OpenNMS alarms on a blink1/mk2 USB led.

Download the blink1 Java-SDK from <http://blink1.thingm.com/libraries/> and build the Java/JNI library:

    # cd blink1/java
    # make jar

And place the libaries in the `./blink1` directory:

    # cd onmsblink/blink1
    # cp /path/to/blink1/libraries/blink1.jar .
    # cp /path/to/blink1/libraries/libBlink1.jnilib .    

Now build _OnmsBlink_:

    # cd onmsblink
    # mvn package
    
Run the tool by invoking `onmsblink.sh`:

    # ./onmsblink.sh [options] baseUrl

The argument `baseUrl` is used to point to the OpenNMS installation. If the argument is not given the default `http://localhost:8980/opennms` will be used. The following options are available:
    

Option             | Description   | Default
------------------ | --------------|--------
 --delay <seconds> | poll delay    | 10000
 --password <text> | password      | admin
 --test            | test and exit | false
 --username <text> | username      | admin