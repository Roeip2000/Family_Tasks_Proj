package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

public class TaskTemplate {
    public String id;
    public String title;
    public String imageBase64;

    public TaskTemplate()
    {

    } // חובה לפיירביס

    public TaskTemplate(String id, String title, String imageBase64)
    {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
    }
}













