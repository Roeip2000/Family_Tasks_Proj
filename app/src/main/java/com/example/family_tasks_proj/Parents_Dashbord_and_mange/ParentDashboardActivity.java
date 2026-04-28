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
        containerFilterOpen.setBackgroundColor(activeFilter == FilterMode.ASSIGNED ? 0x1A000000 : Color.TRANSPARENT);
        containerFilterUrgent.setBackgroundColor(activeFilter == FilterMode.URGENT ? 0x1A000000 : Color.TRANSPARENT);
        containerFilterOverdue.setBackgroundColor(activeFilter == FilterMode.OVERDUE ? 0x1A000000 : Color.TRANSPARENT);
        containerFilterCompleted.setBackgroundColor(activeFilter == FilterMode.COMPLETED ? 0x1A000000 : Color.TRANSPARENT);

        switch (activeFilter) {
            case ASSIGNED: tvTaskSectionTitle.setText("משימות פתוחות"); break;
            case URGENT: tvTaskSectionTitle.setText("משימות דחופות"); break;
            case OVERDUE: tvTaskSectionTitle.setText("משימות באיחור"); break;
            case COMPLETED: tvTaskSectionTitle.setText("משימות שהושלמו"); break;
            default: tvTaskSectionTitle.setText("כל המשימות"); break;
        }
    }

    private void bindActions() {
        btnManageChildren.setOnClickListener(v -> startActivity(new Intent(this, ManageChildrenActivity.class)));
        btnManageTemplates.setOnClickListener(v -> startActivity(new Intent(this, ParentTaskTemplateActivity.class)));
        btnAssignTaskToChild.setOnClickListener(v -> startActivity(new Intent(this, AssignTaskToChildActivity.class)));
        btnShowQR.setOnClickListener(v -> startActivity(new Intent(this, GenerateQRActivity.class)));
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupChildrenList() {
        rvChildren.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(this, childSummaries, id -> {
            selectedChildId = id;
            buildTaskList();
        });
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener((parent, view, position, id) -> showTaskOptionsDialog(position));
    }

    private void loadParentProfile(FirebaseUser user) {
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
            if (matchFilter) visibleTaskItems.add(TaskListItem.createTask(task));
        }
        tvNoTasks.setVisibility(visibleTaskItems.isEmpty() ? View.VISIBLE : View.GONE);
        taskAdapter.notifyDataSetChanged();
        updateListViewHeight(lvTasks);
    }

    private void showTaskOptionsDialog(int position) {
        TaskListItem item = visibleTaskItems.get(position);
        if (item.getIsHeader()) return;
        AssignedTask task = item.getTask();
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setMessage("לילד: " + task.getChildName() + "\nתאריך יעד: " + task.getDueAt())
                .setNeutralButton("מחק משימה", (dialog, which) -> deleteTask(task))
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
                .setPositiveButton("כן", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
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
