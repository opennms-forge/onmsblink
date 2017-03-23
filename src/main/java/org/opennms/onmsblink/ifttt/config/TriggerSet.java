package org.opennms.onmsblink.ifttt.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class TriggerSet {
    /**
     * Name for this trigger set
     */
    private String name;
    /**
     * Triggers to be fired
     */
    private List<Trigger> triggers = new ArrayList<>();

    public TriggerSet() {
    }

    @XmlElement(name = "trigger")
    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
