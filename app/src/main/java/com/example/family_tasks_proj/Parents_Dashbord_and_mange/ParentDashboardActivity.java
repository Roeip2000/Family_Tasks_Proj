package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;

/**
 * דשבורד ראשי של ההורה — מסך Hub.
 *
 * מציג שלושה כפתורי ניווט:
 * 1. ניהול ילדים (ManageChildrenActivity)
 * 2. יצירת תבניות משימה (ParentTaskTemplateActivity)
 * 3. הקצאת משימה לילד (AssignTaskToChildActivity)
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageChildren.setOnClickListener(v ->
        {
            startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
        });

        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnManageTemplates.setOnClickListener(v ->
        {
            startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
        });

        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnAssignTaskToChild.setOnClickListener(v ->
        {
            startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
        });
    }
}