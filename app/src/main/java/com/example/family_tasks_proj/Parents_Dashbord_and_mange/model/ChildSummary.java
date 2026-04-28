package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

// סיכום ספירות משימות לילד אחד
public class ChildSummary {
    private String childId;
    private String displayName;
    private String childProfileBase64;
    private int totalCount;
    private int assignedCount;
    private int completedCount;
    private int urgentCount;
    private int overdueCount;

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getChildProfileBase64() {
        return childProfileBase64;
    }

    public void setChildProfileBase64(String childProfileBase64) {
        this.childProfileBase64 = childProfileBase64;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getUrgentCount() {
        return urgentCount;
    }

    public void setUrgentCount(int urgentCount) {
        this.urgentCount = urgentCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }
}
