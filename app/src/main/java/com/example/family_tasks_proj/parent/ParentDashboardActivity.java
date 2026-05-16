package com.example.family_tasks_proj.parent;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.parent.adapter.ParentDashboardTaskAdapter;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
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
    private TextView tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvFilterTitle;
    private View filterOpen, filterUrgent, filterOverdue, filterDone;
    private RecyclerView rvTasks;

    private final List<AssignedTask> allTasks = new ArrayList<>();
    private final List<AssignedTask> visibleTasks = new ArrayList<>();

    private ParentDashboardTaskAdapter taskAdapter;

    // מצבי הסינון האפשריים בדשבורד ההורה
    private static final int FILTER_NONE = 0;
    private static final int FILTER_OPEN = 1;
    private static final int FILTER_COMPLETED = 2;
    private static final int FILTER_URGENT = 3;
    private static final int FILTER_OVERDUE = 4;
    private int activeFilter = FILTER_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        initViews();
        setupActions();
        setupLists();
        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadData(user);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // חיבור רכיבי הדשבורד מה-XML לקוד
    private void initViews() {
        tvTotal = findViewById(R.id.tvParentTotalTasks);
        tvDone = findViewById(R.id.tvParentCompleted);
        tvUrgent = findViewById(R.id.tvParentDueSoon);
        tvOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvFilterTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);

        filterOpen = findViewById(R.id.containerFilterOpen);
        filterUrgent = findViewById(R.id.containerFilterUrgent);
        filterOverdue = findViewById(R.id.containerFilterOverdue);
        filterDone = findViewById(R.id.containerFilterCompleted);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);
    }

    // הגדרת לחיצה על כרטיסי הסינון בדשבורד
    private void setupFilters() {
        View.OnClickListener filterClick = new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (view == filterOpen)
                {
                    setFilter(FILTER_OPEN);
                } else if (view == filterUrgent)
                {
                    setFilter(FILTER_URGENT);
                } else if (view == filterOverdue)
                {
                    setFilter(FILTER_OVERDUE);
                }
                else if (view == filterDone)
                {
                    setFilter(FILTER_COMPLETED);
                }
            }
        };
        filterOpen.setOnClickListener(filterClick);
        filterUrgent.setOnClickListener(filterClick);
        filterOverdue.setOnClickListener(filterClick);
        filterDone.setOnClickListener(filterClick);
    }

    // שמירת הסינון שנבחר ועדכון הרשימה
    private void setFilter(int selectedFilter) {
        activeFilter = selectedFilter;
        updateFilterUI();
        refreshTaskList();
    }

    // עדכון צבע וכותרת לפי הסינון הפעיל
    private void updateFilterUI() {
        tintFilter(filterOpen, activeFilter == FILTER_OPEN);
        tintFilter(filterUrgent, activeFilter == FILTER_URGENT);
        tintFilter(filterOverdue, activeFilter == FILTER_OVERDUE);
        tintFilter(filterDone, activeFilter == FILTER_COMPLETED);

        if (activeFilter == FILTER_OPEN) {
            tvFilterTitle.setText(R.string.parent_filter_open);
        } else if (activeFilter == FILTER_URGENT) {
            tvFilterTitle.setText(R.string.parent_filter_urgent);
        } else if (activeFilter == FILTER_OVERDUE) {
            tvFilterTitle.setText(R.string.parent_filter_overdue);
        } else if (activeFilter == FILTER_COMPLETED) {
            tvFilterTitle.setText(R.string.parent_filter_completed);
        } else {
            tvFilterTitle.setText("");
        }
    }

    // סימון ויזואלי של פילטר שנבחר
    private void tintFilter(View view, boolean active) {
        if (active) {
            view.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // הגדרת כפתורי המעבר למסכים האחרים
    private void setupActions() {
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
    }

    // הכנת RecyclerView להצגת המשימות המסוננות
    private void setupLists() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTasks);
        rvTasks.setAdapter(taskAdapter);
    }

    // טעינת כל הילדים והמשימות של ההורה מ-Firebase
    private void loadData(FirebaseUser user) {
        DatabaseReference childrenReference = FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children");

        childrenReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();

                int openTasksCount = 0;
                int doneTasksCount = 0;
                int urgentTasksCount = 0;
                int overdueTasksCount = 0;

                // המשימות שמורות תחת כל ילד, לכן עוברים קודם על הילדים
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childName = childSnapshot.child("firstName").getValue(String.class);

                    DataSnapshot tasksSnapshot = childSnapshot.child("tasks");
                    for (DataSnapshot taskSnapshot : tasksSnapshot.getChildren()) {

                        // יצירת אובייקט שמתאים להצגת משימה בדשבורד ההורה
                        AssignedTask task = new AssignedTask();
                        task.setChildName(childName);

                        task.setTitle(taskSnapshot.child("title").getValue(String.class));
                        task.setDueAt(taskSnapshot.child("dueAt").getValue(String.class));
                        task.setImageBase64(taskSnapshot.child("imageBase64").getValue(String.class));

                        Boolean isDone = taskSnapshot.child("isDone").getValue(Boolean.class);
                        if (isDone != null && isDone) {
                            task.setIsDone(true);
                        } else {
                            task.setIsDone(false);
                        }

                        allTasks.add(task);

                        // ספירת המשימה לפי המצב שלה: פתוחה, בוצעה, דחופה או באיחור
                        if (task.getIsDone()) {
                            doneTasksCount++;
                        } else {
                            openTasksCount++;
                            if (DateUtils.isOverdue(task.getDueAt())) {
                                overdueTasksCount++;
                            } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                urgentTasksCount++;
                            }
                        }
                    }
                }
                tvTotal.setText(String.valueOf(openTasksCount));
                tvDone.setText(String.valueOf(doneTasksCount));
                tvUrgent.setText(String.valueOf(urgentTasksCount));
                tvOverdue.setText(String.valueOf(overdueTasksCount));

                refreshTaskList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase דורש את הפעולה הזאת; במקרה של כישלון הדשבורד נשאר כפי שהוא
            }
        });
    }

    // סינון מקומי של המשימות לפי הפילטר שנבחר
    private void refreshTaskList() {
        visibleTasks.clear();

        // בהתחלה לא מציגים משימות עד שההורה בוחר פילטר
        if (activeFilter == FILTER_NONE) {
            tvNoTasks.setVisibility(View.GONE);
            taskAdapter.notifyDataSetChanged();
            return;
        }

        for (int i = 0; i < allTasks.size(); i++) {
            AssignedTask task = allTasks.get(i);
            boolean isMatch = false;

            if (activeFilter == FILTER_OPEN) {
                isMatch = !task.getIsDone();
            } else if (activeFilter == FILTER_COMPLETED) {
                isMatch = task.getIsDone();
            } else if (activeFilter == FILTER_URGENT) {
                isMatch = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt());
            } else if (activeFilter == FILTER_OVERDUE) {
                isMatch = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt());
            }

            if (isMatch) {
                visibleTasks.add(task);
            }
        }

        if (visibleTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
        }

        taskAdapter.notifyDataSetChanged();
    }
}
