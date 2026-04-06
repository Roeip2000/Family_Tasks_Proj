package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

/** שורה ברשימת המשימות — יכולה להיות כותרת קבוצה (header) או משימה רגילה. */
class TaskListItem {
    boolean isHeader;
    String headerTitle;
    int headerCount;
    AssignedTask task;

    static TaskListItem createHeader(String title, int count) {
        TaskListItem item = new TaskListItem();
        item.isHeader = true;
        item.headerTitle = title;
        item.headerCount = count;
        return item;
    }

    static TaskListItem createTask(AssignedTask task) {
        TaskListItem item = new TaskListItem();
        item.task = task;
        return item;
    }
}
