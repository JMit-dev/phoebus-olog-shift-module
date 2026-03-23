package org.phoebus.olog.shift;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Represents a shift as returned by the shift service REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Shift {

    private Integer id;
    private ShiftType type;
    private String owner;
    private Date startDate;
    private Date endDate;
    private String description;
    private String leadOperator;
    private String onShiftPersonal;
    private String report;
    private String closeShiftUser;
    private String status;

    public Shift() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ShiftType getType() {
        return type;
    }

    public void setType(ShiftType type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLeadOperator() {
        return leadOperator;
    }

    public void setLeadOperator(String leadOperator) {
        this.leadOperator = leadOperator;
    }

    public String getOnShiftPersonal() {
        return onShiftPersonal;
    }

    public void setOnShiftPersonal(String onShiftPersonal) {
        this.onShiftPersonal = onShiftPersonal;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getCloseShiftUser() {
        return closeShiftUser;
    }

    public void setCloseShiftUser(String closeShiftUser) {
        this.closeShiftUser = closeShiftUser;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
