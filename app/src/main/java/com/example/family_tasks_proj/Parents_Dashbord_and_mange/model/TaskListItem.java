package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

// שורה ברשימת המשימות — כותרת קבוצה או משימה רגילה
public class TaskListItem {
    private boolean isHeader;
    private String headerTitle;
    private int headerCount;
    private AssignedTask task;

    public boolean getIsHeader() { return isHeader; }
    public void setIsHeader(boolean header) { isHeader = header; }
    public String getHeaderTitle() { return headerTitle; }
    public void setHeaderTitle(String headerTitle) { this.headerTitle = headerTitle; }
    public int getHeaderCount() { return headerCount; }
    public void setHeaderCount(int headerCount) { this.headerCount = headerCount; }
    public AssignedTask getTask() { return task; }
    public void setTask(AssignedTask task) { this.task = task; }

    public static TaskListItem createHeader(String title, int count) {
        TaskListItem item = new TaskListItem();
        item.setIsHeader(true);
        item.setHeaderTitle(title);
        item.setHeaderCount(count);
        return item;
    }

    public static TaskListItem createTask(AssignedTask task) {
        TaskListItem item = new TaskListItem();
        item.setTask(task);
        return item;
    }
}
