package com.example.family_tasks_proj.parent;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// דשבורד ניהול להורה - מציג את כל המשימות של כל הילדים
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvName, tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvFilterTitle;
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

        if (user != null)
        {
            loadProfile(user);
            loadData(user);
        }
        else {
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
        tvName = findViewById(R.id.tvParentName);
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
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == fOpen) {
                    setFilter(FilterMode.ASSIGNED);
                } else if (v == fUrgent) {
                    setFilter(FilterMode.URGENT);
                } else if (v == fOverdue) {
                    setFilter(FilterMode.OVERDUE);
                } else if (v == fDone) {
                    setFilter(FilterMode.COMPLETED);
                }
            }
        };
        fOpen.setOnClickListener(click);
        fUrgent.setOnClickListener(click);
        fOverdue.setOnClickListener(click);
        fDone.setOnClickListener(click);
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
        // צובע את הפילטר הפעיל ברקע אפור שקוף ומאפס את השאר
        tintFilter(fOpen, activeFilter == FilterMode.ASSIGNED);
        tintFilter(fUrgent, activeFilter == FilterMode.URGENT);
        tintFilter(fOverdue, activeFilter == FilterMode.OVERDUE);
        tintFilter(fDone, activeFilter == FilterMode.COMPLETED);

        switch (activeFilter)
        {
            case ASSIGNED:
                tvFilterTitle.setText(R.string.parent_filter_open);
                break;
            case URGENT:
                tvFilterTitle.setText(R.string.parent_filter_urgent);
                break;
            case OVERDUE:
                tvFilterTitle.setText(R.string.parent_filter_overdue);
                break;
            case COMPLETED:
                tvFilterTitle.setText(R.string.parent_filter_completed);
                break;
            default:
                tvFilterTitle.setText(R.string.parent_filter_all);
                break;
        }
    }

    // עוזר לצביעת רקע של כפתור פילטר: אפור שקוף כשפעיל, שקוף לחלוטין אחרת
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
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });
        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });
    }

    private void setupLists() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTasks);
        taskAdapter.setShowChildName(true);
        rvTasks.setAdapter(taskAdapter);
    }

    private void loadProfile(FirebaseUser user) {
        tvName.setText(R.string.parent_greeting);
    }

    private void loadData(FirebaseUser user) {
        // הדשבורד של ההורה עובר על כל הילדים וכל המשימות שלהם.
        // הסיכומים מחושבים מהמשימות עצמן, ולא נשמרים כמונים נפרדים ב-Firebase.
        // מסירים מאזין קודם כדי שלא יהיו כמה מאזינים זהים אחרי חזרה למסך.
        removeChildrenListener();
        childrenReference = FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children");
        childrenListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int totalCount = 0, doneCount = 0, urgentCount = 0, lateCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot cSnap : snapshot.getChildren()) {

                        String cId = cSnap.getKey();
                        String cName = cSnap.child("firstName").getValue(String.class);
                        
                        for (DataSnapshot tSnap : cSnap.child("tasks").getChildren()) {
                            // כל משימה מה-Firebase הופכת לאובייקט Java פשוט להצגה ברשימה.
                            AssignedTask task = new AssignedTask();
                            task.setChildId(cId);
                            if (cName != null) {
                                task.setChildName(cName);
                            } else {
                                task.setChildName(getString(R.string.default_child_name_fallback));
                            }
                            task.setTaskId(tSnap.getKey());
                            task.setTitle(tSnap.child("title").getValue(String.class));
                            task.setDueAt(tSnap.child("dueAt").getValue(String.class));
                            task.setImageBase64(tSnap.child("imageBase64").getValue(String.class));
                            Boolean isDone = tSnap.child("isDone").getValue(Boolean.class);
                            task.setIsDone(isDone != null && isDone);

                            allTasks.add(task);
                            if (task.getIsDone()) {
                                doneCount++;
                            } else {
                                totalCount++;
                                if (DateUtils.isOverdue(task.getDueAt())) {
                                    lateCount++;
                                } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                    urgentCount++;
                                }
                            }
                        }
                    }
                }
                
                tvTotal.setText(String.valueOf(totalCount));
                tvDone.setText(String.valueOf(doneCount));
                tvUrgent.setText(String.valueOf(urgentCount));
                tvOverdue.setText(String.valueOf(lateCount));

                refreshTaskList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // אם טעינת המשימות נכשלה (אין הרשאה/אין רשת) - מציגים הודעה.
                Toast.makeText(ParentDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
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

    private void refreshTaskList() {
        visibleTasks.clear();
        for (AssignedTask task : allTasks) {
            // הפילטר קובע איזו משימה תוצג, בלי למחוק או לשנות את הנתונים המקוריים.
            boolean match = false;
            switch (activeFilter) {
                case ASSIGNED:
                    match = !task.getIsDone();
                    break;
                case COMPLETED:
                    match = task.getIsDone();
                    break;
                case URGENT:
                    match = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt());
                    break;
                case OVERDUE:
                    match = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt());
                    break;
                case ALL:
                default:
                    match = true;
                    break;
            }
            if (match) {
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
