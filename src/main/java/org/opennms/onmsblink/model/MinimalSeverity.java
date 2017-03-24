package org.opennms.onmsblink.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum MinimalSeverity implements Serializable {
    INDETERMINATE(1, "Indeterminate"),
    CLEARED(2, "Cleared"),
    NORMAL(3, "Normal"),
    WARNING(4, "Warning"),
    MINOR(5, "Minor"),
    MAJOR(6, "Major"),
    CRITICAL(7, "Critical");

    private static final Map<Integer, MinimalSeverity> m_idMap;

    private int id;
    private String label;

    static {
        m_idMap = new HashMap<Integer, MinimalSeverity>(values().length);
        for (final MinimalSeverity severity : values()) {
            m_idMap.put(severity.getId(), severity);
        }
    }

    private MinimalSeverity(final int id, final String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLessThan(final MinimalSeverity other) {
        return compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(final MinimalSeverity other) {
        return compareTo(other) <= 0;
    }

    public boolean isGreaterThan(final MinimalSeverity other) {
        return compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(final MinimalSeverity other) {
        return compareTo(other) >= 0;
    }

    public static MinimalSeverity get(final int id) {
        if (m_idMap.containsKey(id)) {
            return m_idMap.get(id);
        } else {
            return null;
        }
    }
}