package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.ChildSummary;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskListItem;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/** מסך ראשי להורה בעיצוב מרכז בקרה נקי. */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR, btnLogout;
    private TextView tvParentName, tvParentTotalTasks, tvParentCompleted, tvParentDueSoon, tvParentOverdue, tvNoTasks, tvTaskSectionTitle;
    private View containerFilterOpen, containerFilterUrgent, containerFilterOverdue, containerFilterCompleted;
    private ListView lvTasks;
    private RecyclerView rvChildren;

    private final List<AssignedTask> allAssignedTasks = new ArrayList<>();
    private final List<TaskListItem> visibleTaskItems = new ArrayList<>();
    private final List<ChildSummary> childSummaries = new ArrayList<>();

    private int houseAssigned, houseDone, houseUrgent, houseOverdue;
    private ParentDashboardTaskAdapter taskAdapter;
    private ParentDashboardChildSummaryAdapter childSummaryAdapter;
    private String selectedChildId = "ALL";
    private FilterMode activeFilter = FilterMode.ALL;

    private enum FilterMode {
        ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);
        bindViews();
        bindActions();
        setupChildrenList();
        setupTaskList();
        setupInteractiveFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // בודק אם יש הורה מחובר. אם לא - מחזיר למסך הכניסה
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadParentProfile(user);
            loadDashboardData(user);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void bindViews() {
        tvParentName = findViewById(R.id.tvParentName);
        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);
        tvParentOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        lvTasks = findViewById(R.id.lvTasks);
        rvChildren = findViewById(R.id.rvChildren);

        containerFilterOpen = findViewById(R.id.containerFilterOpen);
        containerFilterUrgent = findViewById(R.id.containerFilterUrgent);
        containerFilterOverdue = findViewById(R.id.containerFilterOverdue);
        containerFilterCompleted = findViewById(R.id.containerFilterCompleted);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnShowQR = findViewById(R.id.btnShowQR);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupInteractiveFilters() {
        View.OnClickListener filterClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == containerFilterOpen) setActiveFilter(FilterMode.ASSIGNED);
                else if (v == containerFilterUrgent) setActiveFilter(FilterMode.URGENT);
                else if (v == containerFilterOverdue) setActiveFilter(FilterMode.OVERDUE);
                else if (v == containerFilterCompleted) setActiveFilter(FilterMode.COMPLETED);
            }
        };
        containerFilterOpen.setOnClickListener(filterClick);
        containerFilterUrgent.setOnClickListener(filterClick);
        containerFilterOverdue.setOnClickListener(filterClick);
        containerFilterCompleted.setOnClickListener(filterClick);
    }

    private void setActiveFilter(FilterMode mode) {
        if (activeFilter == mode) {
            activeFilter = FilterMode.ALL; // Toggle off to show all
        } else {
            activeFilter = mode;
        }
        updateFilterUi();
        buildTaskList();
    }

    private void updateFilterUi() {
        if (activeFilter == FilterMode.ASSIGNED) {
            containerFilterOpen.setBackgroundColor(0x1A000000);
        } else {
            containerFilterOpen.setBackgroundColor(Color.TRANSPARENT);
        }

        if (activeFilter == FilterMode.URGENT) {
            containerFilterUrgent.setBackgroundColor(0x1A000000);
        } else {
            containerFilterUrgent.setBackgroundColor(Color.TRANSPARENT);
        }

        if (activeFilter == FilterMode.OVERDUE) {
            containerFilterOverdue.setBackgroundColor(0x1A000000);
        } else {
            containerFilterOverdue.setBackgroundColor(Color.TRANSPARENT);
        }

        if (activeFilter == FilterMode.COMPLETED) {
            containerFilterCompleted.setBackgroundColor(0x1A000000);
        } else {
            containerFilterCompleted.setBackgroundColor(Color.TRANSPARENT);
        }

        switch (activeFilter) {
            case ASSIGNED: tvTaskSectionTitle.setText("משימות פתוחות"); break;
            case URGENT: tvTaskSectionTitle.setText("משימות דחופות"); break;
            case OVERDUE: tvTaskSectionTitle.setText("משימות באיחור"); break;
            case COMPLETED: tvTaskSectionTitle.setText("משימות שהושלמו"); break;
            default: tvTaskSectionTitle.setText("כל המשימות"); break;
        }
    }

    private void bindActions() {
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
        btnAssignTaskToChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });
        btnShowQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
    }

    private void setupChildrenList() {
        // מגדיר איך הרשימה תיראה (במקרה הזה - שורה אופקית)
        rvChildren.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // מחבר את המידע מהרשימה לתצוגה שעל המסך בעזרת אדפטר
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(this, childSummaries, new ParentDashboardChildSummaryAdapter.OnChildSelectedListener() {
            @Override
            public void onChildSelected(String id) {
                selectedChildId = id;
                buildTaskList();
            }
        });
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems);
        // מציג את שם הילד ליד כל משימה בדשבורד ההורה
        taskAdapter.setShowChildName(true);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showTaskOptionsDialog(position);
            }
        });
    }

    private void loadParentProfile(FirebaseUser user) {
        // מושך את פרטי ההורה מה-Database לפי ה-UID שלו מה-Auth
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה יקר"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDashboardData(FirebaseUser user) {
        // מאזין לשינויים במידע של הילדים והמשימות ב-Firebase ומתעדכן בזמן אמת
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAssignedTasks.clear();
                childSummaries.clear();
                houseAssigned = houseDone = houseUrgent = houseOverdue = 0;

                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    ChildSummary summary = new ChildSummary();
                    summary.setChildId(childSnap.getKey());
                    summary.setDisplayName(childSnap.child("firstName").getValue(String.class));

                    for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                        AssignedTask task = new AssignedTask();
                        task.setChildId(summary.getChildId());
                        task.setChildName(summary.getDisplayName());
                        task.setTaskId(taskSnap.getKey());
                        task.setTitle(taskSnap.child("title").getValue(String.class));
                        task.setDueAt(taskSnap.child("dueAt").getValue(String.class));
                        task.setImageBase64(taskSnap.child("imageBase64").getValue(String.class));
                        Boolean done = taskSnap.child("isDone").getValue(Boolean.class);
                        task.setIsDone(done != null && done);

                        allAssignedTasks.add(task);
                        if (task.getIsDone()) houseDone++;
                        else {
                            houseAssigned++;
                            if (DateUtils.isOverdue(task.getDueAt())) houseOverdue++;
                            else if (DateUtils.isDueSoon(task.getDueAt())) houseUrgent++;
                        }
                    }
                }
                updateSummaryUi();
                buildTaskList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSummaryUi() {
        tvParentTotalTasks.setText(String.valueOf(houseAssigned));
        tvParentCompleted.setText(String.valueOf(houseDone));
        tvParentDueSoon.setText(String.valueOf(houseUrgent));
        tvParentOverdue.setText(String.valueOf(houseOverdue));
    }

    private void buildTaskList() {
        visibleTaskItems.clear();
        for (AssignedTask task : allAssignedTasks) {
            boolean matchFilter = false;
            switch (activeFilter) {
                case ASSIGNED: matchFilter = !task.getIsDone(); break;
                case COMPLETED: matchFilter = task.getIsDone(); break;
                case URGENT: matchFilter = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt()); break;
                case OVERDUE: matchFilter = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt()); break;
                case ALL: default: matchFilter = true; break;
            }
            if (matchFilter) {
                visibleTaskItems.add(TaskListItem.createTask(task));
            }
        }
        if (visibleTaskItems.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
        }
        taskAdapter.notifyDataSetChanged();
        updateListViewHeight(lvTasks);
    }

    private void showTaskOptionsDialog(int position) {
        TaskListItem item = visibleTaskItems.get(position);
        if (item.getIsHeader()) return;
        final AssignedTask task = item.getTask();
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setMessage("לילד: " + task.getChildName() + "\nתאריך יעד: " + task.getDueAt())
                .setNeutralButton("מחק משימה", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTask(task);
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    private void deleteTask(AssignedTask task) {
        FirebaseDatabase.getInstance().getReference("parents")
                .child(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.getChildId())
                .child("tasks").child(task.getTaskId()).removeValue();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("האם בטוח שברצונך לצאת?")
                .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(ParentDashboardActivity.this, MainActivity.class));
                        finish();
                    }
                }).setNegativeButton("ביטול", null).show();
    }

    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
