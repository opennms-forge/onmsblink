package org.opennms.onmsblink.ifttt;

import java.io.IOException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;

public class IfTttTrigger {
    private String key = "key";
    private String event = "event";
    private String value1 = "";
    private String value2 = "";
    private String value3 = "";
    private boolean quiet = false;

    public IfTttTrigger() {
    }

    public IfTttTrigger key(String key) {
        this.key = key;
        return this;
    }

    public IfTttTrigger quiet(boolean quiet) {
        this.quiet = quiet;
        return this;
    }

    public IfTttTrigger value1(String value1) {
        this.value1 = value1;
        return this;
    }

    public IfTttTrigger value2(String value2) {
        this.value2 = value2;
        return this;
    }

    public IfTttTrigger value3(String value3) {
        this.value3 = value3;
        return this;
    }

    public IfTttTrigger event(String event) {
        this.event = event;
        return this;
    }

    public void trigger() {
        HttpClient httpClient = new HttpClient();
        HostConfiguration hostConfiguration = new HostConfiguration();

        try {
            if (!quiet) {
                System.out.println("ifttt: sending '" + event + "' event to IFTTT...");
            }

            hostConfiguration.setHost(new URI("https://maker.ifttt.com"));
            PostMethod postMethod = new PostMethod("/trigger/" + event + "/with/key/" + key);
            postMethod.setRequestHeader("Content-Type", "application/json");
            String body = "{\"value1\":\"" + value1 + "\",\"value2\":\"" + value2 + "\",\"value3\":\"" + value3 + "\"}";
            postMethod.setRequestBody(body);
            int resultCode = httpClient.executeMethod(hostConfiguration, postMethod);


            if (!quiet && resultCode != 200) {
                System.out.println("ifttt: received HTTP Status " + resultCode + " for request to " + hostConfiguration.getHostURL() + postMethod.getURI() + " with body " + body);
            }

        } catch (URIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}