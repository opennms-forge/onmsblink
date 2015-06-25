# onmsblink
A small tool to indicate OpenNMS alarms on a blink1/mk2 USB led.

In order to compile or execute this tool you need to put the libraries `blink1.jar` and `libBlink1.jnilib` in the `blink1` directory. Please refer to the README.md in the `blink1` directory for instructions.

## Building the tool

You can build _OnmsBlink_ by invoking `mvn package`:

    # cd onmsblink
    # mvn package

## Running the tool
    
Run the tool by invoking `onmsblink.sh`:

    # ./onmsblink.sh --help

### Command line options

The following options are available:
    

Option            | Description             | Default
------------------|-------------------------|--------
--url             | OpenNMS url             | http://localhost:8980/opennms
--delay <seconds> | poll delay in seconds   | 10
--username <text> | username                | admin
--password <text> | password                | admin
--help            | display help and exit   | false
--test            | test and exit           | false
--quiet           | no output except errors | false

### Example 

    # ./onmsblink.sh --quiet --delay 5 --username admin --password secret --url http://opennms.yourdomain.com:8980/opennms 