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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thingm.blink1.Blink1;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class OnmsBlink {

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

    @Option(name = "--help", usage = "display help and exit")
    private boolean help = false;

    private static Logger LOG = LoggerFactory.getLogger(OnmsBlink.class);

    private OnmsSeverity maxSeverity = OnmsSeverity.NORMAL;

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

    public static void main(String[] args) {
        new OnmsBlink(args);
    }

    private void displayHelp(CmdLineParser cmdLineParser) {
        System.err.println("onmsblink - a small tool to indicate OpenNMS alarms on a blink1/mk2 USB led\n");
        cmdLineParser.printUsage(System.err);
    }

    public OnmsBlink(String[] args) {
        final CmdLineParser cmdLineParser = new CmdLineParser(this);
        try {
            cmdLineParser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage() + "\n");

            displayHelp(cmdLineParser);
            System.exit(-1);
        }

        if (help) {
            displayHelp(cmdLineParser);
            System.exit(0);
        }

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Blink1 blink1 = Blink1.open();

                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        blink1.fadeToRGB(0, Color.BLACK);
                        blink1.close();
                    }
                }));

                while (true) {
                    if (maxSeverity.getId() <= 3) {
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
        });

        thread.start();

        if (test) {
            test();
            System.exit(0);
        }

        DefaultApacheHttpClientConfig defaultApacheHttpClientConfig = new DefaultApacheHttpClientConfig();
        defaultApacheHttpClientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        defaultApacheHttpClientConfig.getProperties().put(defaultApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, Boolean.TRUE);
        defaultApacheHttpClientConfig.getState().setCredentials(null, null, -1, username, password);
        ApacheHttpClient apacheHttpClient = ApacheHttpClient.create(defaultApacheHttpClientConfig);

        while (true) {
            WebResource webResource = apacheHttpClient.resource(baseUrl + "/rest/alarms?limit=0&alarmAckTime=null");

            try {
                List<OnmsAlarm> onmsAlarms = webResource.header("Accept", "application/xml").get(new GenericType<List<OnmsAlarm>>() {
                });

                int maxSeverityId = 3;

                for (OnmsAlarm onmsAlarm : onmsAlarms) {
                    maxSeverityId = Math.max(maxSeverityId, onmsAlarm.getSeverityId());
                }

                maxSeverity = OnmsSeverity.get(maxSeverityId);

                System.out.println("Received " + onmsAlarms.size() + " unacknowledged alarm(s), maximum severity is " + maxSeverity.getLabel());

            } catch (Exception ex) {
                LOG.debug("Error executing rest request", ex);
            }

            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void test() {
        for (int i = 3; i < 8; i++) {
            maxSeverity = OnmsSeverity.get(i);

            System.out.println("Setting LED to severity " + maxSeverity.getLabel());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
