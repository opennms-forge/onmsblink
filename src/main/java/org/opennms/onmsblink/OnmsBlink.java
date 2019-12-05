package org.opennms.onmsblink;

import java.awt.Color;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXB;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.onmsblink.ifttt.IfTttTrigger;
import org.opennms.onmsblink.ifttt.config.IfTttConfig;
import org.opennms.onmsblink.ifttt.config.Trigger;
import org.opennms.onmsblink.ifttt.config.TriggerSet;
import org.opennms.onmsblink.model.MinimalCategory;
import org.opennms.onmsblink.model.MinimalNode;
import org.opennms.onmsblink.model.MinimalAlarm;
import org.opennms.onmsblink.model.MinimalSeverity;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import thingm.blink1.Blink1;

/**
 * onmsblink - a small tool for indicating unacknowledged alarms on your blink1/mk2 USB led.
 *
 * @author Christian Pape
 */
public class OnmsBlink {

    /**
     * Helper interface for replacing variables in commands and values
     */
    private interface VariableNameExpansion {
        String replace(String string);
    }

    /**
     * Default implementation
     */
    private static class DefaultVariableNameExpansion implements VariableNameExpansion {
        MinimalSeverity oldSeverity, newSeverity;
        int oldAlarmCount, newAlarmCount;

        public DefaultVariableNameExpansion(MinimalSeverity oldSeverity, MinimalSeverity newSeverity, int oldAlarmCount, int newAlarmCount) {
            this.oldSeverity = oldSeverity;
            this.newSeverity = newSeverity;
            this.oldAlarmCount = oldAlarmCount;
            this.newAlarmCount = newAlarmCount;
        }

        @Override
        public String replace(String string) {
            return string.replace("%os%", oldSeverity.toString())
                    .replace("%ns%", newSeverity.toString())
                    .replace("%oc%", String.valueOf(oldAlarmCount))
                    .replace("%nc%", String.valueOf(newAlarmCount));
        }
    }

    /**
     * Worker class
     */
    private static class OnmsBlinkWorker implements Runnable {
        /**
         * error constant
         */
        private static final int BLINK1_ERROR = -1;
        /**
         * millis seconds constants
         */
        private static final int BLINK1_LOW_DELAY = 50;
        private static final int BLINK1_MEDIUM_DELAY = 75;
        private static final int BLINK1_HIGH_DELAY = 100;
        /**
         * the blink1 API instance
         */
        private Blink1 blink1 = Blink1.open();
        /**
         * the maximum severity of the requested alarms
         */
        private MinimalSeverity maxSeverity = MinimalSeverity.NORMAL;
        /**
         * constant glow
         */
        private boolean constant = false;
        /**
         * Map for mapping severities to colors
         */
        private HashMap<MinimalSeverity, Color> colorMap = new HashMap<>();

        {
            colorMap.put(MinimalSeverity.INDETERMINATE, new Color(0x336600));
            colorMap.put(MinimalSeverity.CLEARED, new Color(0x336600));
            colorMap.put(MinimalSeverity.NORMAL, new Color(0x336600));
            colorMap.put(MinimalSeverity.WARNING, new Color(0xFFCC00));
            colorMap.put(MinimalSeverity.MINOR, new Color(0xFF9900));
            colorMap.put(MinimalSeverity.MAJOR, new Color(0xCC3300));
            colorMap.put(MinimalSeverity.CRITICAL, new Color(0xFF0000));
        }


        public OnmsBlinkWorker() {
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

        public MinimalSeverity getMaxSeverity() {
            return maxSeverity;
        }

        public void setMaxSeverity(final MinimalSeverity maxSeverity) {
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
                if (maxSeverity.isLessThanOrEqual(MinimalSeverity.NORMAL) || isConstantGlow()) {
                    if (blink1.fadeToRGB(0, colorMap.get(maxSeverity)) == BLINK1_ERROR) {
                        blink1 = Blink1.open();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    int speed = MinimalSeverity.CRITICAL.getId() - Math.max(maxSeverity.getId(), MinimalSeverity.NORMAL.getId());

                    if (blink1.fadeToRGB((1 + speed) * BLINK1_HIGH_DELAY, colorMap.get(maxSeverity)) == BLINK1_ERROR) {
                        blink1 = Blink1.open();
                    }

                    try {
                        Thread.sleep((1 + speed) * BLINK1_MEDIUM_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (blink1.fadeToRGB((1 + speed) * BLINK1_MEDIUM_DELAY, Color.BLACK) == BLINK1_ERROR) {
                        blink1 = Blink1.open();
                    }

                    try {
                        Thread.sleep((1 + speed) * BLINK1_LOW_DELAY);
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

    @Option(required = false, name = "--username", usage = "username, if not set OnmsBlink will prompt for username")
    private String username = null;

    @Option(required = false, name = "--password", usage = "password, if not set OnmsBlink will prompt for password")
    private String password = null;

    @Option(name = "--test", usage = "test and exit")
    private boolean test = false;

    @Option(name = "--constant", usage = "constant glow, no flashing")
    public boolean constant = false;

    @Option(name = "--help", usage = "display help and exit")
    private boolean help = false;

    @Option(name = "--quiet", usage = "no output when polling, only errors")
    private boolean quiet = false;

    @Option(name = "--execute", usage = "execute program on status change (placeholders are %os% for old severity, %ns% for new severity, %oc% for old alarm count and %nc% for new alarm count)")
    private String execute = null;

    @Option(name = "--ifttt", usage = "trigger IFTTT events defined in ifttt-config.xml")
    public boolean ifttt = false;

    @Option(required = false, name = "--category", usage = "category filter (Java regular-expression)")
    private String categoryFilter = null;

    @Option(name = "--ignore-certificates", usage = "do not validate any SSL certificates")
    public boolean ignoreCertificates = false;

    /**
     * IfTtt config
     */
    private IfTttConfig ifTttConfig = null;

    /**
     * Node cache
     */
    private Map<Integer, Boolean> nodeCache;

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

    private void fireIfTttTriggerSet(MinimalSeverity newSeverity, VariableNameExpansion variableNameExpansion) {
        fireIfTttTriggerSet(newSeverity.getLabel(), variableNameExpansion);
    }

    private void fireIfTttTriggerSet(String name) {
        fireIfTttTriggerSet(name, new VariableNameExpansion() {
            @Override
            public String replace(String string) {
                return string;
            }
        });
    }

    private void fireIfTttTriggerSet(String name, VariableNameExpansion variableNameExpansion) {
        if (!ifttt || ifTttConfig == null) {
            return;
        }

        TriggerSet triggerSet = ifTttConfig.getTriggerSetForName(name);

        if (triggerSet != null) {
            for (Trigger trigger : triggerSet.getTriggers()) {

                new IfTttTrigger()
                        .key(ifTttConfig.getKey())
                        .event(trigger.getEventName())
                        .value1(variableNameExpansion.replace(trigger.getValue1()))
                        .value2(variableNameExpansion.replace(trigger.getValue2()))
                        .value3(variableNameExpansion.replace(trigger.getValue3()))
                        .quiet(quiet)
                        .trigger();

                try {
                    Thread.sleep(trigger.getDelay());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (!quiet) {
                System.out.println("ifttt: no trigger-set with name '" + name + "' defined");
            }
        }
    }

    public void execute() {
        if (ifttt) {
            if (!quiet) {
                System.out.println("ifttt: loading configuration file ifttt-config.xml");
            }

            ifTttConfig = JAXB.unmarshal(new File("ifttt-config.xml"), IfTttConfig.class);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    fireIfTttTriggerSet("off");
                }
            }));

            fireIfTttTriggerSet("on");
        }

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
                onmsBlinkWorker.setMaxSeverity(MinimalSeverity.get(i));
                fireIfTttTriggerSet(MinimalSeverity.get(i), new DefaultVariableNameExpansion(MinimalSeverity.get(i - 1), MinimalSeverity.get(i), 0, 1));

                if (!quiet) {
                    System.out.println("test: setting LED to severity " + onmsBlinkWorker.getMaxSeverity().getLabel());
                }

                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            fireIfTttTriggerSet("off");

            System.exit(0);
        }

        while (username == null || "".equals(username)) {
            username = System.console().readLine("Username: ");
        }

        while (password == null || "".equals(password)) {
            password = new String(System.console().readPassword("Password: "));
        }

        /**
         * Initialize the rest stuff
         */
        final DefaultApacheHttpClientConfig defaultApacheHttpClientConfig = new DefaultApacheHttpClientConfig();
        defaultApacheHttpClientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        defaultApacheHttpClientConfig.getProperties().put(defaultApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, Boolean.TRUE);
        defaultApacheHttpClientConfig.getState().setCredentials(null, null, -1, username, password);

        if (ignoreCertificates) {
            ignoreCertificates(defaultApacheHttpClientConfig);
        }
        
        final ApacheHttpClient apacheHttpClient = ApacheHttpClient.create(defaultApacheHttpClientConfig);

        /**
         * Loop for requesting the alarms and setting the maxSeverity field.
         */

        int oldAlarmCount = 0;
        MinimalSeverity oldSeverity = MinimalSeverity.INDETERMINATE;

        while (true) {
            final WebResource alarmsWebResource = apacheHttpClient.resource(baseUrl + "/rest/alarms?comparator=gt&severity=NORMAL&alarmAckTime=null&limit=0");

            nodeCache = new HashMap<>();

            try {
                final List<MinimalAlarm> minimalAlarms = alarmsWebResource.header("Accept", "application/xml").get(new GenericType<List<MinimalAlarm>>() {
                });

                MinimalSeverity newSeverity = MinimalSeverity.NORMAL;
                int newAlarmCount = 0;

                /**
                 * iterate over all alarms
                 */
                for (final MinimalAlarm minimalAlarm : minimalAlarms) {
                    boolean alarmMatches = true;
                    /**
                     * should we filter for categories?
                     */
                    if (categoryFilter != null) {

                        alarmMatches = false;

                        /**
                         * check for existence of the nodeId
                         */
                        if (minimalAlarm.getNodeId() != null) {

                            /**
                             * check for already queried node in cache
                             */
                            if (nodeCache.containsKey(minimalAlarm.getNodeId())) {
                                alarmMatches = nodeCache.get(minimalAlarm.getNodeId());
                            } else {
                                /**
                                 * query for node resource
                                 */
                                final WebResource nodeWebResource = apacheHttpClient.resource(baseUrl + "/rest/nodes/" + minimalAlarm.getNodeId());
                                final MinimalNode minimalNode = nodeWebResource.header("Accept", "application/xml").get(new GenericType<MinimalNode>() {
                                });

                                if (minimalNode != null) {
                                    /**
                                     * check for categories
                                     */
                                    if (minimalNode.getCategories() != null) {
                                        nodeCache.put(minimalNode.getId(), false);
                                        /**
                                         * iterate over categories and check for match
                                         */
                                        for (MinimalCategory minimalCategory : minimalNode.getCategories()) {
                                            if (minimalCategory.getName().matches(categoryFilter)) {
                                                nodeCache.put(minimalNode.getId(), true);
                                                alarmMatches = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (alarmMatches) {
                        if (MinimalSeverity.valueOf(minimalAlarm.getSeverity()).isGreaterThan(newSeverity)) {
                            newSeverity = MinimalSeverity.valueOf(minimalAlarm.getSeverity());
                        }
                        newAlarmCount++;
                    }
                }

                onmsBlinkWorker.setMaxSeverity(newSeverity);

                DefaultVariableNameExpansion defaultVariableNameExpansion = new DefaultVariableNameExpansion(oldSeverity, newSeverity, oldAlarmCount, newAlarmCount);

                if (!newSeverity.equals(oldSeverity) || newAlarmCount != oldAlarmCount) {
                    fireIfTttTriggerSet(newSeverity, defaultVariableNameExpansion);

                    if (execute != null) {
                        ProcessBuilder processBuilder = new ProcessBuilder();

                        String executeWithArguments = defaultVariableNameExpansion.replace(execute);

                        if (!quiet) {
                            System.out.println("execute: executing '" + executeWithArguments + "'");
                        }

                        Process process = (quiet ? processBuilder.command("sh", "-c", executeWithArguments).start()
                                : processBuilder.command("sh", "-c", executeWithArguments).inheritIO().start());

                        int resultCode = process.waitFor();

                        if (!quiet && resultCode != 0) {
                            System.out.println("execute: execution of '" + executeWithArguments + "' returned non-null value " + resultCode);
                        }
                    }
                }

                if (!quiet) {
                    System.out.println("opennms: received " + newAlarmCount + " unacknowledged alarm(s) with severity > Normal, maximum severity is " + onmsBlinkWorker.getMaxSeverity().getLabel());
                }

                oldSeverity = newSeverity;
                oldAlarmCount = newAlarmCount;
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private void ignoreCertificates(final DefaultApacheHttpClientConfig defaultApacheHttpClientConfig) {
        final TrustManager[ ] certs = new TrustManager[ ] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                }
        };

        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (java.security.GeneralSecurityException ex) {
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        try {
            defaultApacheHttpClientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    },
                    ctx
            ));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
