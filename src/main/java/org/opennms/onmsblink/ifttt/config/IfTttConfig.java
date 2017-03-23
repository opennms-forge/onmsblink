package org.opennms.onmsblink.ifttt.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ifttt-config")
public class IfTttConfig {
    /**
     * IFTTT key of the maker service
     */
    private String key;
    /**
     * Trigger sets for firing IFTTT events
     */
    private List<TriggerSet> triggerSets = new ArrayList<>();

    public IfTttConfig() {
    }

    @XmlAttribute
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name="trigger-set")
    public List<TriggerSet> getTriggerSets() {
        return triggerSets;
    }

    public void setTriggerSets(List<TriggerSet> triggerSets) {
        this.triggerSets = triggerSets;
    }

    public TriggerSet getTriggerSetForName(String name) {
        for(TriggerSet triggerSet : triggerSets) {
            if (name.toLowerCase().equals(triggerSet.getName().toLowerCase())) {
                return triggerSet;
            }
        }
        return null;
    }
}
