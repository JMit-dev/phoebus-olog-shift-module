package org.phoebus.shift.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Shift {

    private Integer id;
    private ShiftType type;
    private String owner;
    private String status;
    private Date startDate;
    private Date endDate;
    private String description;
    private String leadOperator;

    public Shift() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public ShiftType getType() { return type; }
    public void setType(ShiftType type) { this.type = type; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLeadOperator() { return leadOperator; }
    public void setLeadOperator(String leadOperator) { this.leadOperator = leadOperator; }
}
