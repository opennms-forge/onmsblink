/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
                System.out.println("Sending '" + event + "' event to IFTTT...");
            }

            hostConfiguration.setHost(new URI("https://maker.ifttt.com"));
            PostMethod postMethod = new PostMethod("/trigger/" + event + "/with/key/" + key);
            postMethod.setRequestHeader("Content-Type", "application/json");
            String body = "{\"value1\":\"" + value1 + "\",\"value2\":\"" + value2 + "\",\"value3\":\"" + value3 + "\"}";
            postMethod.setRequestBody(body);
            int resultCode = httpClient.executeMethod(hostConfiguration, postMethod);


            if (!quiet && resultCode != 200) {
                System.out.println("Received HTTP Status "+resultCode+", for request:");
                System.out.println(hostConfiguration.getHostURL() + postMethod.getURI());
                System.out.println(body);
            }

        } catch (URIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}