package org.opennms.onmsblink.ifttt.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Trigger {
    /**
     * The IFTTT event to send
     */
    private String eventName = "";
    /**
     * The value1 to be used
     */
    private String value1 = "";
    /**
     * The value2 to be used
     */
    private String value2 = "";
    /**
     * The value3 to be used
     */
    private String value3 = "";
    /**
     * Delay after executing the trigger
     */
    private int delay;

    public Trigger() {
    }

    @XmlAttribute(name = "eventName")
    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @XmlElement(name = "value1")
    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    @XmlElement(name = "value2")
    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    @XmlElement(name = "value3")
    public String getValue3() {
        return value3;
    }

    public void setValue3(String value3) {
        this.value3 = value3;
    }

    @XmlAttribute
    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
