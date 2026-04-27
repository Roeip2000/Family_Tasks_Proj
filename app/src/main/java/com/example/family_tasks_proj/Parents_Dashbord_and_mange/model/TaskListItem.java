package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

// שורה ברשימת המשימות — כותרת קבוצה או משימה רגילה
public class TaskListItem {
    public boolean isHeader;
    public String headerTitle;
    public int headerCount;
    public AssignedTask task;

    public static TaskListItem createHeader(String title, int count) {
        TaskListItem item = new TaskListItem();
        item.isHeader = true;
        item.headerTitle = title;
        item.headerCount = count;
        return item;
    }

    public static TaskListItem createTask(AssignedTask task) {
        TaskListItem item = new TaskListItem();
        item.task = task;
        return item;
    }
}
