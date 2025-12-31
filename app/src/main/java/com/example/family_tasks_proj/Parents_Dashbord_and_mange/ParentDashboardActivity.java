package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;

public class ParentDashboardActivity extends AppCompatActivity {
    Button btnManageChildren;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);



        btnManageChildren = findViewById(R.id.btnManageChildren);

        btnManageChildren.setOnClickListener(v -> {
            startActivity(new Intent(
                    ParentDashboardActivity.this,
                    ManageChildrenActivity.class
            ));
        });

    }
}
