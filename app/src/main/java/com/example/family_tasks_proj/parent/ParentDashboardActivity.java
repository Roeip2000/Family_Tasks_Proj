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
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.parent.adapter.ParentDashboardTaskAdapter;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;

    private final List<AssignedTask> openTasks = new ArrayList<>();

    private ParentDashboardTaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        initViews();
        setupActions();
        setupLists();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // מחזור חיים: בכל חזרה למסך בודקים אם ההורה עדיין מחובר.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null)
        {
            loadData(user);
        }
        else
        {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // חיבור רכיבי המסך מה-XML לקוד.
    private void initViews()
    {
        tvTotal = findViewById(R.id.tvParentTotalTasks);
        tvDone = findViewById(R.id.tvParentCompleted);
        tvUrgent = findViewById(R.id.tvParentDueSoon);
        tvOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        tvTaskSectionTitle.setText(R.string.parent_filter_open);
        rvTasks = findViewById(R.id.rvTasks);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);
    }

    // הגדרת כפתורי המעבר למסכים האחרים.
    private void setupActions()
    {
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });

        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });

        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });

        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });
    }

    // הכנת RecyclerView להצגת המשימות הפתוחות.
    private void setupLists()
    {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, openTasks);
        rvTasks.setAdapter(taskAdapter);
    }

    private void loadData(FirebaseUser user)
    {
        openTasks.clear();
        tvNoTasks.setVisibility(View.GONE);
        taskAdapter.notifyDataSetChanged();

        // נתיב Firebase: לכל הורה יש רשימת ילדים, ולכל ילד יש משימות.
        DatabaseReference childrenReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children");

        childrenReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                openTasks.clear();

                // מונים: סופרים כמה משימות יש בכל מצב להצגה בכרטיסי הסיכום.
                int openTasksCount = 0;
                int doneTasksCount = 0;
                int urgentTasksCount = 0;
                int overdueTasksCount = 0;

                // מעבר על הילדים ועל המשימות שלהם מתוך Firebase.
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    String childName = childSnapshot.child("firstName").getValue(String.class);
                    DataSnapshot tasksSnapshot = childSnapshot.child("tasks");

                    for (DataSnapshot taskSnapshot : tasksSnapshot.getChildren())
                    {
                        AssignedTask task = new AssignedTask();
                        task.setChildName(childName);
                        task.setTitle(taskSnapshot.child("title").getValue(String.class));
                        task.setDueAt(taskSnapshot.child("dueAt").getValue(String.class));
                        task.setImageBase64(taskSnapshot.child("imageBase64").getValue(String.class));

                        Boolean isDone = taskSnapshot.child("isDone").getValue(Boolean.class);
                        task.setIsDone(isDone != null && isDone);

                        if (task.getIsDone())
                        {
                            doneTasksCount++;
                        }
                        else
                        {
                            openTasksCount++;
                            openTasks.add(task);

                            if (DateUtils.isOverdue(task.getDueAt()))
                            {
                                overdueTasksCount++;
                            }
                            else if (DateUtils.isDueSoon(task.getDueAt()))
                            {
                                urgentTasksCount++;
                            }
                        }
                    }
                }

                tvTotal.setText(String.valueOf(openTasksCount));
                tvDone.setText(String.valueOf(doneTasksCount));
                tvUrgent.setText(String.valueOf(urgentTasksCount));
                tvOverdue.setText(String.valueOf(overdueTasksCount));

                // עדכון RecyclerView: מציגים רק את רשימת המשימות הפתוחות.
                if (openTasks.isEmpty())
                {
                    tvNoTasks.setVisibility(View.VISIBLE);
                }
                else
                {
                    tvNoTasks.setVisibility(View.GONE);
                }

                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                // במקרה של שגיאה לא משנים את המסך.
            }
        });
    }
}
