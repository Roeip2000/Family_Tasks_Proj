package com.example.family_tasks_proj.Child_Login;

public class Child {
    public String firstName;
    public String lastName;

    public Child() {
        // Default constructor required for calls to DataSnapshot.getValue(Child.class)
    }

    public Child(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
