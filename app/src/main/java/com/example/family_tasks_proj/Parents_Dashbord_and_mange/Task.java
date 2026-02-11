package com.example.family_tasks_proj.Parents_Dashbord_and_mange;


public class Task {
    public String title;
    public String assignedTo;
    public String dueDate;
    public boolean completed;

    public Task() {}

    public Task(String title, String assignedTo, String dueDate, boolean completed)
    {
        this.title = title;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.completed = completed;
    }
}

