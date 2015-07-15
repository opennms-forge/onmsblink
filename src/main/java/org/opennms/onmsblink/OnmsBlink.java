package org.opennms.onmsblink;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsSeverity;
import thingm.blink1.Blink1;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

/**
 * onmsblink - a small tool for indicating unacknowledged alarms on your blink1/mk2 USB led.
 *
 * @author Christian Pape
 */
public class OnmsBlink {
    /**
     * Worker class
     */
    private static class OnmsBlinkWorker implements Runnable {
        /**
         * the blink1 API instance
         */
        private final Blink1 blink1 = Blink1.open();
        /**
         * the maximum severity of the requested alarms
         */
        private OnmsSeverity maxSeverity = OnmsSeverity.NORMAL;

        /**
         * constant glow
         */
        private boolean constant = false;

        /**
         * Map for mapping severities to colors
         */
        private HashMap<OnmsSeverity, Color> colorMap = new HashMap<>();

        {
            colorMap.put(OnmsSeverity.INDETERMINATE, new Color(0x336600));
            colorMap.put(OnmsSeverity.CLEARED, new Color(0x336600));
            colorMap.put(OnmsSeverity.NORMAL, new Color(0x336600));
            colorMap.put(OnmsSeverity.WARNING, new Color(0xFFCC00));
            colorMap.put(OnmsSeverity.MINOR, new Color(0xFF9900));
            colorMap.put(OnmsSeverity.MAJOR, new Color(0xCC3300));
            colorMap.put(OnmsSeverity.CRITICAL, new Color(0xFF0000));
        }

        public OnmsBlinkWorker() {
            /**
             * check for devices...
             */
            if (blink1.getCount() == 0) {
                System.err.println("Sorry, no devices found!");
                System.exit(-1);
            }

            /**
             * add exit hook for shutting down the blink1
             */
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    blink1.fadeToRGB(0, Color.BLACK);
                    blink1.close();
                }
            }));
        }

        public OnmsSeverity getMaxSeverity() {
            return maxSeverity;
        }

        public void setMaxSeverity(final OnmsSeverity maxSeverity) {
            this.maxSeverity = maxSeverity;
        }

        public boolean isConstantGlow() {
            return constant;
        }

        public void setConstantGlow(boolean constant) {
            this.constant = constant;
        }

        @Override
        public void run() {
            while (true) {
                if (maxSeverity.getId() <= 3 || isConstantGlow()) {
                    blink1.fadeToRGB(0, colorMap.get(maxSeverity));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    int speed = 7 - Math.max(maxSeverity.getId(), 3);

                    blink1.fadeToRGB(100 + speed * 100, colorMap.get(maxSeverity));

                    try {
                        Thread.sleep(75 + 75 * speed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    blink1.fadeToRGB(75 + speed * 75, Color.BLACK);

                    try {
                        Thread.sleep(100 + 50 * speed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Command line options
     */
    @Option(required = false, name = "--url", usage = "URL of your OpenNMS installation")
    private String baseUrl = "http://localhost:8980/opennms";

    @Option(required = false, name = "--delay", usage = "poll delay in seconds")
    private Integer delay = 10;

    @Option(required = false, name = "--username", usage = "username")
    private String username = "admin";

    @Option(required = false, name = "--password", usage = "password")
    private String password = "admin";

    @Option(name = "--test", usage = "test and exit")
    private boolean test = false;

    @Option(name = "--constant", usage = "constant glow, no flashing")
    public boolean constant = false;

    @Option(name = "--help", usage = "display help and exit")
    private boolean help = false;

    @Option(name = "--quiet", usage = "no output when polling, only errors")
    private boolean quiet = false;

    /**
     * Constructor for instantiating new objects of this class.
     */
    public OnmsBlink() {
    }

    /**
     * Main Method
     *
     * @param args command line parameters
     */
    public static void main(final String[] args) {
        OnmsBlink onmsBlink = new OnmsBlink();
        onmsBlink.parseArguments(args);
        onmsBlink.execute();
    }

    /**
     * Displays the usage help message.
     *
     * @param cmdLineParser the parser instance to be used
     */
    private void displayHelp(final CmdLineParser cmdLineParser) {
        System.err.println("onmsblink - a small tool to indicate OpenNMS alarms on a blink1/mk2 USB led\n");
        cmdLineParser.printUsage(System.err);
    }

    /**
     * Parses the command line arguments.
     *
     * @param args the array of arguments
     */
    public void parseArguments(final String args[]) {
        /**
         * Parse the arguments
         */
        final CmdLineParser cmdLineParser = new CmdLineParser(this);

        try {
            cmdLineParser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage() + "\n");

            displayHelp(cmdLineParser);
            System.exit(-1);
        }

        /**
         * Display help message if "--help" was used
         */
        if (help) {
            displayHelp(cmdLineParser);
            System.exit(0);
        }
    }

    public void execute() {
        /**
         * Create a thread for cycling through the colors
         */
        final OnmsBlinkWorker onmsBlinkWorker = new OnmsBlinkWorker();
        onmsBlinkWorker.setConstantGlow(constant);
        final Thread thread = new Thread(onmsBlinkWorker);
        thread.start();

        /**
         * if "--test" is given cycle through the severities and exit
         */
        if (test) {
            for (int i = 3; i < 8; i++) {
                onmsBlinkWorker.setMaxSeverity(OnmsSeverity.get(i));

                if (!quiet) {
                    System.out.println("Setting LED to severity " + onmsBlinkWorker.getMaxSeverity().getLabel());
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.exit(0);
        }

        /**
         * Initialize the rest stuff
         */
        final DefaultApacheHttpClientConfig defaultApacheHttpClientConfig = new DefaultApacheHttpClientConfig();
        defaultApacheHttpClientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        defaultApacheHttpClientConfig.getProperties().put(defaultApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, Boolean.TRUE);
        defaultApacheHttpClientConfig.getState().setCredentials(null, null, -1, username, password);
        final ApacheHttpClient apacheHttpClient = ApacheHttpClient.create(defaultApacheHttpClientConfig);

        /**
         * Loop for requesting the alarms and setting the maxSeverity field.
         */
        while (true) {
            final WebResource webResource = apacheHttpClient.resource(baseUrl + "/rest/alarms?comparator=gt&severity=NORMAL&alarmAckTime=null&limit=0");

            try {
                final List<OnmsAlarm> onmsAlarms = webResource.header("Accept", "application/xml").get(new GenericType<List<OnmsAlarm>>() {
                });

                int maxSeverityId = 3;

                for (final OnmsAlarm onmsAlarm : onmsAlarms) {
                    maxSeverityId = Math.max(maxSeverityId, onmsAlarm.getSeverityId());
                }

                onmsBlinkWorker.setMaxSeverity(OnmsSeverity.get(maxSeverityId));

                if (!quiet) {
                    System.out.println("Received " + onmsAlarms.size() + " unacknowledged alarm(s) with severity > Normal, maximum severity is " + onmsBlinkWorker.getMaxSeverity().getLabel());
                }

            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }

            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
