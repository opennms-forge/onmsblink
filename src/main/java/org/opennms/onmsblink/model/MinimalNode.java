package org.opennms.onmsblink.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "node")
public class MinimalNode {
    private Integer id;
    private String label;
    private Set<MinimalCategory> categories = new LinkedHashSet<>();

    public MinimalNode() {
    }

    @XmlAttribute
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlAttribute
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Set<MinimalCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<MinimalCategory> categories) {
        this.categories = categories;
    }
}
