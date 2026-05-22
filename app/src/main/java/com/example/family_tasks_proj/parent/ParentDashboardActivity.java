package com.example.family_tasks_proj.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.parent.adapter.ParentDashboardTaskAdapter;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvOpenTasksCount, tvDoneTasksCount, tvUrgentTasksCount, tvOverdueTasksCount;
    private RecyclerView rvTasks;

    private final List<AssignedTask> openTasks = new ArrayList<>();
    private ParentDashboardTaskAdapter taskAdapter;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        parentId = FirebaseAuth.getInstance().getUid();

        tvOpenTasksCount = findViewById(R.id.tvParentTotalTasks);
        tvDoneTasksCount = findViewById(R.id.tvParentCompleted);
        tvUrgentTasksCount = findViewById(R.id.tvParentDueSoon);
        tvOverdueTasksCount = findViewById(R.id.tvParentOverdue);
        rvTasks = findViewById(R.id.rvTasks);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);

        // חיבור כפתורי הדשבורד למסכים המתאימים
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });

        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });

        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });

        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, openTasks);
        rvTasks.setAdapter(taskAdapter);

        loadDashboardData();
    }

    private void loadDashboardData() {
        DatabaseReference childrenRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children");

        childrenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openTasks.clear();
                int openCount = 0;
                int doneCount = 0;
                int urgentCount = 0;
                int overdueCount = 0;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childName = childSnapshot.child("firstName").getValue(String.class);
                    DataSnapshot tasksSnapshot = childSnapshot.child("tasks");

                    for (DataSnapshot taskSnapshot : tasksSnapshot.getChildren()) {
                        AssignedTask task = new AssignedTask();

                        task.setChildName(childName);
                        task.setTitle(taskSnapshot.child("title").getValue(String.class));
                        task.setDueAt(taskSnapshot.child("dueAt").getValue(String.class));
                        task.setImageBase64(taskSnapshot.child("imageBase64").getValue(String.class));
                        
                        Boolean isDone = taskSnapshot.child("isDone").getValue(Boolean.class);
                        task.setIsDone(isDone != null && isDone);

                        if (task.getIsDone()) {
                            doneCount++;
                        } else {
                            openCount++;
                            openTasks.add(task);

                            // שימוש במחלקת העזר DateUtils לבדיקת תאריך היעד
                            if (DateUtils.isOverdue(task.getDueAt())) {
                                overdueCount++;
                            } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                urgentCount++;
                            }
                        }
                    }
                }

                tvOpenTasksCount.setText(String.valueOf(openCount));
                tvDoneTasksCount.setText(String.valueOf(doneCount));
                tvUrgentTasksCount.setText(String.valueOf(urgentCount));
                tvOverdueTasksCount.setText(String.valueOf(overdueCount));
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
