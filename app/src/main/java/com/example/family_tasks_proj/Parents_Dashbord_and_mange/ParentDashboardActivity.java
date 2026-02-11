package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;

public class ParentDashboardActivity extends AppCompatActivity {

    // הגדרת המשתנים עבור הכפתורים החדשים מה-XML
    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // 1. כפתור ניהול ילדים
        btnManageChildren = findViewById(R.id.btnManageChildren);

        btnManageChildren.setOnClickListener(v ->
        {
            startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
        });

        // 2. כפתור ניהול מאגר תבניות (בנק משימות)

        btnManageTemplates = findViewById(R.id.btnManageTemplates);

        btnManageTemplates.setOnClickListener(v ->
        {
            // מוביל לדף שבו יוצרים את התבניות עם ה-Base64
            startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));

        });


        // 3. כפתור שלח משימה לילד מהמאגר (הקצאה)
        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnAssignTaskToChild.setOnClickListener(v ->
        {
            // מוביל לדף ההופכי שבו בוחרים תבנית, ילד ותאריך
            startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
        });
    }
}