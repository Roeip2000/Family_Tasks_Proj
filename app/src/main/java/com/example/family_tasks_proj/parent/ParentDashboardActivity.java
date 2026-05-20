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

    // רכיבי ממשק המשתמש (UI Elements)
    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvGreeting, tvOpenTasksCount, tvDoneTasksCount, tvUrgentTasksCount, tvOverdueTasksCount, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;

    // רשימה שתכיל רק את המשימות הפתוחות להצגה במסך
    private final List<AssignedTask> openTasks = new ArrayList<>();

    // מתאם (Adapter) לקישור בין רשימת המשימות ל-RecyclerView
    private ParentDashboardTaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור רכיבי הדשבורד מה-XML (Binding)
        tvGreeting = findViewById(R.id.tvParentName);
        tvGreeting.setText(R.string.parent_greeting);
        tvOpenTasksCount = findViewById(R.id.tvParentTotalTasks);
        tvDoneTasksCount = findViewById(R.id.tvParentCompleted);
        tvUrgentTasksCount = findViewById(R.id.tvParentDueSoon);
        tvOverdueTasksCount = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);

        // קביעת כותרת למקטע המשימות הפתוחות
        tvTaskSectionTitle.setText(R.string.parent_open_tasks_title);

        // כפתור למעבר למסך ניהול הילדים
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });

        // כפתור למעבר למסך ניהול תבניות
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });

        // כפתור למעבר למסך שיוך משימה לילד
        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });

        // כפתור למעבר למסך הפקת קוד QR
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });

        // הגדרת ה-RecyclerView וחיבור המתאם (Adapter)
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, openTasks);
        rvTasks.setAdapter(taskAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // בדיקה האם ההורה מחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadData(user); // טעינת נתונים מה-Firebase
        } else {
            // אם לא מחובר, חזרה למסך הראשי
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * טעינת נתוני הילדים והמשימות שלהם מה-Firebase
     */
    private void loadData(FirebaseUser user) {
        // איפוס הרשימה לפני טעינה חדשה
        openTasks.clear();
        tvNoTasks.setVisibility(View.GONE);
        taskAdapter.notifyDataSetChanged();

        // הפניה לנתיב הילדים של ההורה ב-Firebase
        DatabaseReference childrenReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children");

        // קריאה חד פעמית של נתוני הילדים
        childrenReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openTasks.clear();

                // מונים לסטטיסטיקות שמוצגות בראש המסך
                int openTasksCount = 0;
                int doneTasksCount = 0;
                int urgentTasksCount = 0;
                int overdueTasksCount = 0;

                // מעבר על כל הילדים
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childName = childSnapshot.child("firstName").getValue(String.class);
                    DataSnapshot tasksSnapshot = childSnapshot.child("tasks");

                    // מעבר על כל המשימות של הילד
                    for (DataSnapshot taskSnapshot : tasksSnapshot.getChildren()) {
                        AssignedTask task = new AssignedTask();

                        task.setChildName(childName);
                        task.setTitle(taskSnapshot.child("title").getValue(String.class));
                        task.setDueAt(taskSnapshot.child("dueAt").getValue(String.class));
                        task.setImageBase64(taskSnapshot.child("imageBase64").getValue(String.class));
                        
                        Boolean isDone = taskSnapshot.child("isDone").getValue(Boolean.class);
                        task.setIsDone(isDone != null && isDone);

                        // ספירה ומיון המשימות
                        if (task.getIsDone()) {
                            doneTasksCount++;
                        } else {
                            openTasksCount++;
                            openTasks.add(task); // הצגת משימות פתוחות בלבד

                            if (DateUtils.isOverdue(task.getDueAt())) {
                                overdueTasksCount++;
                            } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                urgentTasksCount++;
                            }
                        }
                    }
                }

                // עדכון המספרים בתצוגה
                tvOpenTasksCount.setText(String.valueOf(openTasksCount));
                tvDoneTasksCount.setText(String.valueOf(doneTasksCount));
                tvUrgentTasksCount.setText(String.valueOf(urgentTasksCount));
                tvOverdueTasksCount.setText(String.valueOf(overdueTasksCount));

                // הצגת הודעה אם אין משימות פתוחות
                tvNoTasks.setVisibility(openTasks.isEmpty() ? View.VISIBLE : View.GONE);

                // רענון הרשימה (RecyclerView)
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // טיפול בשגיאה (לא נדרש למימוש כרגע)
            }
        });
    }
}
