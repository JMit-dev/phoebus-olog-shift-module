package org.phoebus.olog.shift;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the type of a shift as returned by the shift service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftType {

    private int id;
    private String name;

    public ShiftType() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
