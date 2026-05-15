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
    private View fOpen, fUrgent, fOverdue, fDone;
    private RecyclerView rvTasks;

    private final List<AssignedTask> allTasks = new ArrayList<>();
    private final List<AssignedTask> visibleTasks = new ArrayList<>();

    private ParentDashboardTaskAdapter taskAdapter;
    private FilterMode activeFilter = FilterMode.ALL;
    private DatabaseReference childrenReference;
    private ValueEventListener childrenListener;

    private enum FilterMode { ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE }

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

    @Override
    protected void onStop() {
        super.onStop();
        removeChildrenListener();
    }

    private void initViews() {
        tvTotal = findViewById(R.id.tvParentTotalTasks);
        tvDone = findViewById(R.id.tvParentCompleted);
        tvUrgent = findViewById(R.id.tvParentDueSoon);
        tvOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvFilterTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);

        fOpen = findViewById(R.id.containerFilterOpen);
        fUrgent = findViewById(R.id.containerFilterUrgent);
        fOverdue = findViewById(R.id.containerFilterOverdue);
        fDone = findViewById(R.id.containerFilterCompleted);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);
    }

    private void setupFilters() {
        View.OnClickListener filterClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == fOpen) {
                    setFilter(FilterMode.ASSIGNED);
                } else if (view == fUrgent) {
                    setFilter(FilterMode.URGENT);
                } else if (view == fOverdue) {
                    setFilter(FilterMode.OVERDUE);
                } else if (view == fDone) {
                    setFilter(FilterMode.COMPLETED);
                }
            }
        };
        fOpen.setOnClickListener(filterClick);
        fUrgent.setOnClickListener(filterClick);
        fOverdue.setOnClickListener(filterClick);
        fDone.setOnClickListener(filterClick);
    }

    private void setFilter(FilterMode mode) {
        if (activeFilter == mode) {
            activeFilter = FilterMode.ALL;
        } else {
            activeFilter = mode;
        }
        updateFilterUI();
        refreshTaskList();
    }

    private void updateFilterUI() {
        tintFilter(fOpen, activeFilter == FilterMode.ASSIGNED);
        tintFilter(fUrgent, activeFilter == FilterMode.URGENT);
        tintFilter(fOverdue, activeFilter == FilterMode.OVERDUE);
        tintFilter(fDone, activeFilter == FilterMode.COMPLETED);

        if (activeFilter == FilterMode.ASSIGNED) {
            tvFilterTitle.setText(R.string.parent_filter_open);
        } else if (activeFilter == FilterMode.URGENT) {
            tvFilterTitle.setText(R.string.parent_filter_urgent);
        } else if (activeFilter == FilterMode.OVERDUE) {
            tvFilterTitle.setText(R.string.parent_filter_overdue);
        } else if (activeFilter == FilterMode.COMPLETED) {
            tvFilterTitle.setText(R.string.parent_filter_completed);
        } else {
            tvFilterTitle.setText(R.string.parent_filter_all);
        }
    }

    private void tintFilter(View view, boolean active) {
        if (active) {
            view.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

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

    private void setupLists() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTasks);
        rvTasks.setAdapter(taskAdapter);
    }

    // קריאת כל הילדים והמשימות מהמסד כדי להציג בדשבורד המרכזי
    private void loadData(FirebaseUser user) {
        removeChildrenListener();
        
        childrenReference = FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children");
        
        childrenListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                
                int openCount = 0;
                int doneCount = 0;
                int urgentCount = 0;
                int lateCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot childSnap : snapshot.getChildren()) {
                        String childName = childSnap.child("firstName").getValue(String.class);
                        
                        DataSnapshot tasksSnap = childSnap.child("tasks");
                        for (DataSnapshot tSnap : tasksSnap.getChildren()) {
                            
                            AssignedTask task = new AssignedTask();

                            if (childName != null) {
                                task.setChildName(childName);
                            } else {
                                task.setChildName(getString(R.string.default_child_name_fallback));
                            }

                            task.setTitle(tSnap.child("title").getValue(String.class));
                            task.setDueAt(tSnap.child("dueAt").getValue(String.class));
                            task.setImageBase64(tSnap.child("imageBase64").getValue(String.class));
                            
                            Boolean isDone = tSnap.child("isDone").getValue(Boolean.class);
                            if (isDone != null && isDone) {
                                task.setIsDone(true);
                            } else {
                                task.setIsDone(false);
                            }

                            allTasks.add(task);

                            if (task.getIsDone()) {
                                doneCount++;
                            } else {
                                openCount++;
                                if (DateUtils.isOverdue(task.getDueAt())) {
                                    lateCount++;
                                } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                    urgentCount++;
                                }
                            }
                        }
                    }
                }
                
                tvTotal.setText(String.valueOf(openCount));
                tvDone.setText(String.valueOf(doneCount));
                tvUrgent.setText(String.valueOf(urgentCount));
                tvOverdue.setText(String.valueOf(lateCount));

                refreshTaskList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        childrenReference.addValueEventListener(childrenListener);
    }

    private void removeChildrenListener() {
        if (childrenReference != null && childrenListener != null) {
            childrenReference.removeEventListener(childrenListener);
        }
        childrenReference = null;
        childrenListener = null;
    }

    // סינון מקומי של הרשימה שמוצגת ב-RecyclerView
    private void refreshTaskList() {
        visibleTasks.clear();
        
        for (int i = 0; i < allTasks.size(); i++) {
            AssignedTask task = allTasks.get(i);
            boolean isMatch = false;
            
            if (activeFilter == FilterMode.ASSIGNED) {
                isMatch = !task.getIsDone();
            } else if (activeFilter == FilterMode.COMPLETED) {
                isMatch = task.getIsDone();
            } else if (activeFilter == FilterMode.URGENT) {
                isMatch = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt());
            } else if (activeFilter == FilterMode.OVERDUE) {
                isMatch = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt());
            } else {
                isMatch = true;
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

